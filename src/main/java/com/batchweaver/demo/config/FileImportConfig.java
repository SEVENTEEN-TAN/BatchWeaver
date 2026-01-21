package com.batchweaver.demo.config;

import com.batchweaver.core.fileprocess.function.FooterParser;
import com.batchweaver.core.fileprocess.function.FooterValidator;
import com.batchweaver.core.fileprocess.function.HeaderParser;
import com.batchweaver.core.fileprocess.function.HeaderValidator;
import com.batchweaver.core.fileprocess.model.FooterInfo;
import com.batchweaver.core.fileprocess.model.HeaderInfo;
import com.batchweaver.demo.shared.entity.DemoUser;
import com.batchweaver.core.fileprocess.template.FileImportJobTemplate;
import com.batchweaver.demo.shared.entity.DemoUserInput;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import com.batchweaver.core.fileprocess.reader.HeaderFooterAwareReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Job3: 文件导入测试配置
 * <p>
 * 测试目的：验证不同格式的文件导入功能
 * 支持3种格式：
 * - Format1: yyyyMMdd + 纯数字 Footer
 * - Format2: MMddyyyy + R前缀 Footer
 * - Format3: 无头尾
 */
@Configuration
public class FileImportConfig {

    // =============================================================
    // Format1: yyyyMMdd + 纯数字 Footer
    // =============================================================

    /**
     * Format1 导入 Job
     * <p>
     * 格式：yyyyMMdd + 数据行 + count
     */
    @Bean
    public Job format1ImportJob(
            JobRepository jobRepository,
            PlatformTransactionManager tm2,
            ItemProcessor<DemoUserInput, DemoUser> demoUserInputToDemoUserNoIdProcessor,
            ItemWriter<DemoUser> db2DemoUserWriter) {

        Resource resource = new FileSystemResource("data/input/format1_users.txt");

        // Header 解析：yyyyMMdd
        HeaderParser headerParser = line -> {
            LocalDate date = LocalDate.parse(line.trim(), DateTimeFormatter.ofPattern("yyyyMMdd"));
            return new HeaderInfo(date);
        };

        // Header 校验：日期必须匹配（或使用 Job 参数）
        HeaderValidator headerValidator = header -> {
            // 实际使用中可通过 Job 参数传入期望日期
            LocalDate today = LocalDate.now();
            if (!header.getDate().equals(today)) {
                System.out.println("Header date mismatch: expected=" + today + ", actual=" + header.getDate());
            }
        };

        // Footer 解析：纯数字
        FooterParser footerParser = line -> {
            long count = Long.parseLong(line.trim());
            return new FooterInfo(count);
        };

        // Footer 校验：数量匹配
        FooterValidator footerValidator = (footer, actual) -> {
            if (footer.getCount() != actual) {
                throw new IllegalStateException("Count mismatch: expected=" + footer.getCount() + ", actual=" + actual);
            }
        };

        FileImportJobTemplate template = new FileImportJobTemplate();

        // LineTokenizer
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setNames("name", "age", "email", "birthDate");

        // FieldSetMapper
        BeanWrapperFieldSetMapper<DemoUserInput> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(DemoUserInput.class);

        // Reader - 使用 HeaderFooterAwareReader
        HeaderFooterAwareReader<DemoUserInput> reader = new HeaderFooterAwareReader<>(
                resource,
                headerParser,
                headerValidator,
                footerParser,
                footerValidator,
                lineTokenizer,
                fieldSetMapper
        );

        // 构建 Job 定义
        FileImportJobTemplate.FileImportJobDefinition<DemoUserInput, DemoUser> definition =
                FileImportJobTemplate.FileImportJobDefinition.<DemoUserInput, DemoUser>builder()
                        .jobName("format1ImportJob")
                        .stepName("format1ImportStep")
                        .jobRepository(jobRepository)
                        .transactionManager(tm2)
                        .reader(reader)
                        .processor(demoUserInputToDemoUserNoIdProcessor)
                        .writer(db2DemoUserWriter)
                        .chunkSize(100)
                        .skipLimit(100)
                        .retryLimit(3)
                        .build();

        return template.buildJob(definition);
    }

    // =============================================================
    // Format2: MMddyyyy + R前缀 Footer
    // =============================================================

    /**
     * Format2 导入 Job
     * <p>
     * 格式：MMddyyyy + 数据行 + R00003
     */
    @Bean
    public Job format2ImportJob(
            JobRepository jobRepository,
            PlatformTransactionManager tm2,
            ItemProcessor<DemoUserInput, DemoUser> demoUserInputToDemoUserCopyIdProcessor,
            ItemWriter<DemoUser> db2DemoUserWriter) {

        Resource resource = new FileSystemResource("data/input/format2_users.txt");

        // Header 解析：MMddyyyy
        HeaderParser headerParser = line -> {
            LocalDate date = LocalDate.parse(line.trim(), DateTimeFormatter.ofPattern("MMddyyyy"));
            return new HeaderInfo(date);
        };

        // Footer 解析：R前缀
        FooterParser footerParser = line -> {
            String countStr = line.trim().substring(1); // 去掉 R
            long count = Long.parseLong(countStr);
            return new FooterInfo(count);
        };

        // Footer 校验：数量匹配
        FooterValidator footerValidator = (footer, actual) -> {
            if (footer.getCount() != actual) {
                throw new IllegalStateException("Count mismatch: expected=" + footer.getCount() + ", actual=" + actual);
            }
        };

        FileImportJobTemplate template = new FileImportJobTemplate();

        // LineTokenizer
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setNames("name", "age", "email", "birthDate");

        // FieldSetMapper
        BeanWrapperFieldSetMapper<DemoUserInput> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(DemoUserInput.class);

        // Reader - 使用 HeaderFooterAwareReader
        HeaderFooterAwareReader<DemoUserInput> reader = new HeaderFooterAwareReader<>(
                resource,
                headerParser,
                null,  // 没有 headerValidator
                footerParser,
                footerValidator,
                lineTokenizer,
                fieldSetMapper
        );

        // 构建 Job 定义
        FileImportJobTemplate.FileImportJobDefinition<DemoUserInput, DemoUser> definition =
                FileImportJobTemplate.FileImportJobDefinition.<DemoUserInput, DemoUser>builder()
                        .jobName("format2ImportJob")
                        .stepName("format2ImportStep")
                        .jobRepository(jobRepository)
                        .transactionManager(tm2)
                        .reader(reader)
                        .processor(demoUserInputToDemoUserCopyIdProcessor)
                        .writer(db2DemoUserWriter)
                        .chunkSize(100)
                        .skipLimit(100)
                        .retryLimit(3)
                        .build();

        return template.buildJob(definition);
    }

    // =============================================================
    // Format3: 无头尾
    // =============================================================

    /**
     * Format3 导入 Job
     * <p>
     * 格式：纯数据行（无 Header/Footer）
     */
    @Bean
    public Job format3ImportJob(
            JobRepository jobRepository,
            PlatformTransactionManager tm2,
            ItemProcessor<DemoUserInput, DemoUser> demoUserInputToDemoUserCopyIdProcessor,
            ItemWriter<DemoUser> db2DemoUserWriter) {

        Resource resource = new FileSystemResource("data/input/format3_users.txt");

        // Reader
        FlatFileItemReader<DemoUserInput> reader = new FlatFileItemReaderBuilder<DemoUserInput>()
                .name("format3Reader")
                .resource(resource)
                .delimited()
                .delimiter(",")
                .names("name", "age", "email", "birthDate")
                .targetType(DemoUserInput.class)
                .build();

        // 构建 Step
        Step step = new StepBuilder("format3ImportStep", jobRepository)
                .<DemoUserInput, DemoUser>chunk(100, tm2)
                .reader(reader)
                .processor(demoUserInputToDemoUserCopyIdProcessor)
                .writer(db2DemoUserWriter)
                .faultTolerant()
                .skipLimit(100)
                .retryLimit(3)
                .build();

        // 构建 Job
        return new JobBuilder("format3ImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step)
                .build();
    }

}
