package com.batchweaver.demo.config;

import com.batchweaver.batch.entity.DemoUser;
import com.batchweaver.batch.service.Db2BusinessService;
import com.batchweaver.core.fileprocess.function.FooterParser;
import com.batchweaver.core.fileprocess.function.FooterValidator;
import com.batchweaver.core.fileprocess.function.HeaderParser;
import com.batchweaver.core.fileprocess.model.FooterInfo;
import com.batchweaver.core.fileprocess.model.HeaderInfo;
import com.batchweaver.core.fileprocess.reader.HeaderFooterAwareReader;
import com.batchweaver.demo.shared.entity.DemoUserInput;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Job1: 条件流程测试配置
 * <p>
 * 测试目的：验证 Spring Batch 的条件分支功能
 * 文件格式：H|yyyyMMdd|USER_IMPORT + 数据 + T|count
 * <p>
 * 注：Footer 处理通过 StepExecutionListener 验证记录数量
 */
@Configuration
public class ConditionalFlowConfig {

    @Value("${batch.weaver.demo.input-path}data/input/")
    private String inputPath;

    @Value("${batch.weaver.demo.files.demo}")
    private String demoFileName;

    @Value("${batch.weaver.demo.chunk-size:100}")
    private int chunkSize;

    /**
     * 条件流程 Job
     * <p>
     * 流程：conditionalImportStep → resultDecider → successStep OR failureStep
     */
    @Bean
    public Job conditionalFlowJob(
            JobRepository jobRepository,
            Step conditionalImportStep,
            JobExecutionDecider resultDecider,
            Step successStep,
            Step failureStep) {

        return new JobBuilder("conditionalFlowJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(conditionalImportStep)
                .next(resultDecider)
                .on("COMPLETED").to(successStep)
                .on("COMPLETED_WITH_SKIPS").to(successStep)
                .on("FAILED").to(failureStep)
                .from(resultDecider).on("*").stop()
                .end()
                .build();
    }

    /**
     * 导入 Step
     */
    @Bean
    public Step conditionalImportStep(
            JobRepository jobRepository,
            @Qualifier("tm2") PlatformTransactionManager tm2,
            ItemReader<DemoUserInput> reader,
            ItemProcessor<DemoUserInput, DemoUser> processor,
            ItemWriter<DemoUser> writer) {

        return new StepBuilder("conditionalImportStep", jobRepository)
                .<DemoUserInput, DemoUser>chunk(chunkSize, tm2)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(10)
                .skip(org.springframework.batch.item.file.FlatFileParseException.class)
                .skip(java.lang.NumberFormatException.class)
                .skip(java.time.format.DateTimeParseException.class)
                .build();
    }

    /**
     * 结果决策器
     * <p>
     * 根据 skip 计数决定路由
     */
    @Bean
    public JobExecutionDecider resultDecider() {
        return (jobExecution, stepExecution) -> {
            long skipCount = stepExecution.getSkipCount();

            if (stepExecution.getStatus().name().equals("COMPLETED")) {
                if (skipCount > 0) {
                    System.out.println("Job completed with " + skipCount + " skips");
                    return new FlowExecutionStatus("COMPLETED_WITH_SKIPS");
                }
                return new FlowExecutionStatus("COMPLETED");
            } else if (stepExecution.getStatus().name().equals("FAILED")) {
                return new FlowExecutionStatus("FAILED");
            }

            return new FlowExecutionStatus("UNKNOWN");
        };
    }

    /**
     * 成功分支 Step
     */
    @Bean
    public Step successStep(
            JobRepository jobRepository,
            @Qualifier("tm2") PlatformTransactionManager tm2) {

        return new StepBuilder("successStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("========================================");
                    System.out.println("[SUCCESS BRANCH] Job executed successfully");
                    System.out.println("========================================");
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, tm2)
                .build();
    }

    /**
     * 失败分支 Step
     */
    @Bean
    public Step failureStep(
            JobRepository jobRepository,
            @Qualifier("tm2") PlatformTransactionManager tm2) {

        return new StepBuilder("failureStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("========================================");
                    System.out.println("[FAILURE BRANCH] Job execution failed");
                    System.out.println("========================================");
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, tm2)
                .build();
    }

    /**
     * Reader：读取 demo_users.txt
     * <p>
     * 格式：H|yyyyMMdd|type + 数据行 + T|count
     */
    @Bean
    public HeaderFooterAwareReader<DemoUserInput> conditionalDemoReader() {
        Resource resource = new FileSystemResource(inputPath + demoFileName);

        // Header 解析：H|yyyyMMdd|type
        HeaderParser headerParser = line -> {
            String[] parts = line.split("\\|");
            if (parts.length >= 2) {
                LocalDate date = LocalDate.parse(parts[1].trim(), DateTimeFormatter.ofPattern("yyyyMMdd"));
                return new HeaderInfo(date);
            }
            throw new IllegalArgumentException("Invalid header format: " + line);
        };

        // Footer 解析：T|count
        FooterParser footerParser = line -> {
            String[] parts = line.split("\\|");
            if (parts.length >= 2) {
                long count = Long.parseLong(parts[1].trim());
                return new FooterInfo(count);
            }
            throw new IllegalArgumentException("Invalid footer format: " + line);
        };

        // Footer 校验：数量必须匹配
        FooterValidator footerValidator = (footer, actual) -> {
            if (footer.getCount() != actual) {
                throw new IllegalStateException("Footer count mismatch: expected=" + footer.getCount() + ", actual=" + actual);
            }
        };

        // LineTokenizer
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter("|");
        lineTokenizer.setNames("id", "name", "age", "email", "birthDate");

        // FieldSetMapper
        BeanWrapperFieldSetMapper<DemoUserInput> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(DemoUserInput.class);

        // 使用 HeaderFooterAwareReader
        return new HeaderFooterAwareReader<>(
                resource,
                headerParser,
                null,  // 没有 headerValidator
                footerParser,
                footerValidator,
                lineTokenizer,
                fieldSetMapper
        );
    }

    /**
     * Processor：DemoUserInput → DemoUser（忽略 age 字段）
     */
    @Bean
    public ItemProcessor<DemoUserInput, DemoUser> demoUserProcessor() {
        return input -> {
            DemoUser user = new DemoUser();
            user.setId(input.getId());
            user.setName(input.getName());
            user.setEmail(input.getEmail());
            user.setBirthDate(input.getBirthDate());
            // age 字段被忽略
            return user;
        };
    }

    /**
     * Writer：写入 DB2
     */
    @Bean
    public ItemWriter<DemoUser> demoUserWriter(Db2BusinessService db2BusinessService) {
        return items -> db2BusinessService.batchInsertUsers(new java.util.ArrayList<>(items.getItems()));
    }
}
