package com.batchweaver.demo.config;

import com.batchweaver.batch.entity.DemoUser;
import com.batchweaver.batch.service.Db2BusinessService;
import com.batchweaver.demo.shared.entity.DemoUserInput;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Job2: 批处理模式测试配置
 * <p>
 * 测试目的：验证基于 Chunk 的批处理模式
 * 文件格式：yyyyMMdd + 数据行 + count
 */
@Configuration
public class ChunkProcessingConfig {

    @Value("${batch.weaver.demo.input-path}data/input/")
    private String inputPath;

    @Value("${batch.weaver.demo.files.large:large_users.txt}")
    private String largeFileName;

    @Value("${batch.weaver.demo.chunk-size:100}")
    private int chunkSize;

    /**
     * Chunk 模式 Job
     */
    @Bean
    public Job chunkProcessingJob(
            JobRepository jobRepository,
            Step chunkProcessingStep) {

        return new JobBuilder("chunkProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(chunkProcessingStep)
                .build();
    }

    /**
     * Chunk 处理 Step
     */
    @Bean
    public Step chunkProcessingStep(
            JobRepository jobRepository,
            @Qualifier("tm2") PlatformTransactionManager tm2,
            ItemReader<DemoUserInput> reader,
            ItemProcessor<DemoUserInput, DemoUser> processor,
            ItemWriter<DemoUser> writer) {

        return new StepBuilder("chunkProcessingStep", jobRepository)
                .<DemoUserInput, DemoUser>chunk(chunkSize, tm2)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(100)
                .skip(org.springframework.batch.item.file.FlatFileParseException.class)
                .skip(java.lang.NumberFormatException.class)
                .skip(java.time.format.DateTimeParseException.class)
                .listener(chunkExecutionListener())
                .listener(new StepExecutionListenerImpl())
                .build();
    }

    /**
     * Reader：读取 large_users.txt
     */
    @Bean
    public FlatFileItemReader<DemoUserInput> largeFileReader() {
        return new FlatFileItemReaderBuilder<DemoUserInput>()
                .name("largeFileReader")
                .resource(new FileSystemResource(inputPath + largeFileName))
                .linesToSkip(1)  // 跳过 Header 行
                .delimited()
                .delimiter(",")
                .names("name", "age", "email", "birthDate")
                .targetType(DemoUserInput.class)
                .build();
    }

    /**
     * Processor：DemoUserInput → DemoUser
     */
    @Bean
    public ItemProcessor<DemoUserInput, DemoUser> chunkUserProcessor() {
        return input -> {
            DemoUser user = new DemoUser();
            // 大文件没有 id，需要生成
            user.setId(null);  // 由数据库自增或序列生成
            user.setName(input.getName());
            user.setEmail(input.getEmail());
            user.setBirthDate(input.getBirthDate());
            return user;
        };
    }

    /**
     * Writer：写入 DB2
     */
    @Bean
    public ItemWriter<DemoUser> chunkUserWriter(Db2BusinessService db2BusinessService) {
        return items -> db2BusinessService.batchInsertUsers(new java.util.ArrayList<>(items.getItems()));
    }

    /**
     * Chunk 执行监听器
     */
    @Bean
    public ChunkExecutionListener chunkExecutionListener() {
        return new ChunkExecutionListener();
    }

    /**
     * Chunk 执行监听器实现
     */
    public static class ChunkExecutionListener implements org.springframework.batch.core.ChunkListener {
        @Override
        public void beforeChunk(org.springframework.batch.core.scope.context.ChunkContext context) {
            System.out.println("========================================");
            System.out.println("[CHUNK] Starting chunk: " + context.getAttribute("chunk.count"));
        }

        @Override
        public void afterChunk(org.springframework.batch.core.scope.context.ChunkContext context) {
            System.out.println("[CHUNK] Chunk completed");
            System.out.println("========================================");
        }
    }

    /**
     * Step 执行监听器实现
     */
    public static class StepExecutionListenerImpl implements org.springframework.batch.core.StepExecutionListener {
        @Override
        public void beforeStep(org.springframework.batch.core.StepExecution stepExecution) {
            System.out.println("[CHUNK] Step started");
        }

        @Override
        public org.springframework.batch.core.ExitStatus afterStep(org.springframework.batch.core.StepExecution stepExecution) {
            System.out.println("[CHUNK] Step completed. Read: " + stepExecution.getReadCount()
                    + ", Written: " + stepExecution.getWriteCount()
                    + ", Skipped: " + stepExecution.getSkipCount());
            return stepExecution.getExitStatus();
        }
    }
}
