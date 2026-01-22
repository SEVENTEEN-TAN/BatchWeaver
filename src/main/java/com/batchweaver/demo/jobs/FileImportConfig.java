package com.batchweaver.demo.jobs;

import com.batchweaver.core.fileprocess.function.FooterParser;
import com.batchweaver.core.fileprocess.function.FooterValidator;
import com.batchweaver.core.fileprocess.function.HeaderParser;
import com.batchweaver.core.fileprocess.function.HeaderValidator;
import com.batchweaver.core.fileprocess.model.FooterInfo;
import com.batchweaver.core.fileprocess.model.HeaderInfo;
import com.batchweaver.core.reader.AnnotationDrivenFieldSetMapper;
import com.batchweaver.demo.entity.ChunkUserInput;
import com.batchweaver.demo.entity.DemoUser;
import com.batchweaver.demo.entity.DemoUserInput;
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
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
    // Master Job: 串行执行所有格式导入
    // =============================================================

    /**
     * Master 导入 Job - 串行执行所有格式
     * <p>
     * 执行顺序：format1 -> format2 -> format3
     */
    @Bean
    public Job masterImportJob(
            JobRepository jobRepository,
            Step format1ImportStep,
            Step format2ImportStep,
            Step format3ImportStep) {

        return new JobBuilder("masterImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(format1ImportStep)
                .next(format2ImportStep)
                .next(format3ImportStep)
                .build();
    }

    // =============================================================
    // Format1: yyyyMMdd + 纯数字 Footer
    // =============================================================

    /**
     * Format1 导入 Step
     */
    @Bean
    public Step format1ImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager tm2,
            ItemProcessor<ChunkUserInput, DemoUser> demoUserInputToDemoUserNoIdProcessor,
            ItemWriter<DemoUser> db2DemoUserWriter) {

        Resource resource = new FileSystemResource("data/input/format1_users.txt");

        // Header 解析：yyyyMMdd
        HeaderParser headerParser = line -> {
            LocalDate date = LocalDate.parse(line.trim(), DateTimeFormatter.ofPattern("yyyyMMdd"));
            return new HeaderInfo(date);
        };

        // Header 校验：日期必须匹配（或使用 Job 参数）
        HeaderValidator headerValidator = header -> {
            LocalDate today = LocalDate.now();
            if (!header.getDate().equals(today)) {
                System.out.println("[Format1] Header date mismatch: expected=" + today + ", actual=" + header.getDate());
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
                throw new IllegalStateException("[Format1] Count mismatch: expected=" + footer.getCount() + ", actual=" + actual);
            }
        };

        // LineTokenizer
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setNames("name", "age", "email", "birthDate");

        // FieldSetMapper
        AnnotationDrivenFieldSetMapper<ChunkUserInput> fieldSetMapper = new AnnotationDrivenFieldSetMapper<>(ChunkUserInput.class);


        // Reader
        HeaderFooterAwareReader<ChunkUserInput> reader = new HeaderFooterAwareReader<>(
                resource,
                headerParser,
                headerValidator,
                footerParser,
                footerValidator,
                lineTokenizer,
                fieldSetMapper
        );

        return new StepBuilder("format1ImportStep", jobRepository)
                .<ChunkUserInput, DemoUser>chunk(100, tm2)
                .reader(reader)
                .processor(demoUserInputToDemoUserNoIdProcessor)
                .writer(db2DemoUserWriter)
                .faultTolerant()
                .skipLimit(100)
                .skip(FlatFileParseException.class)
                .skip(NumberFormatException.class)
                .build();
    }

    /**
     * Format1 导入 Job (独立执行)
     */
    @Bean
    public Job format1ImportJob(
            JobRepository jobRepository,
            Step format1ImportStep) {

        return new JobBuilder("format1ImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(format1ImportStep)
                .build();
    }

    // =============================================================
    // Format2: MMddyyyy + R前缀 Footer
    // =============================================================

    /**
     * Format2 导入 Step
     */
    @Bean
    public Step format2ImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager tm2,
            ItemProcessor<DemoUserInput, DemoUser> demoUserInputToDemoUserNoIdProcessor,
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
                throw new IllegalStateException("[Format2] Count mismatch: expected=" + footer.getCount() + ", actual=" + actual);
            }
        };

        // LineTokenizer
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setNames("id","name", "age", "email", "birthDate");

        // FieldSetMapper
        AnnotationDrivenFieldSetMapper<DemoUserInput> fieldSetMapper = new AnnotationDrivenFieldSetMapper<>(DemoUserInput.class);

        // Reader
        HeaderFooterAwareReader<DemoUserInput> reader = new HeaderFooterAwareReader<>(
                resource,
                headerParser,
                null,  // 没有 headerValidator
                footerParser,
                footerValidator,
                lineTokenizer,
                fieldSetMapper
        );

        return new StepBuilder("format2ImportStep", jobRepository)
                .<DemoUserInput, DemoUser>chunk(100, tm2)
                .reader(reader)
                .processor(demoUserInputToDemoUserNoIdProcessor)
                .writer(db2DemoUserWriter)
                .faultTolerant()
                .skipLimit(100)
                .skip(FlatFileParseException.class)
                .skip(NumberFormatException.class)
                .build();
    }

    /**
     * Format2 导入 Job (独立执行)
     */
    @Bean
    public Job format2ImportJob(
            JobRepository jobRepository,
            Step format2ImportStep) {

        return new JobBuilder("format2ImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(format2ImportStep)
                .build();
    }

    // =============================================================
    // Format3: 无头尾
    // =============================================================

    /**
     * Format3 导入 Step
     */
    @Bean
    public Step format3ImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager tm2,
            ItemProcessor<ChunkUserInput, DemoUser> demoUserInputToDemoUserNoIdProcessor,
            ItemWriter<DemoUser> db2DemoUserWriter) {

        Resource resource = new FileSystemResource("data/input/format3_users.txt");

        // LineTokenizer
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setNames("name", "age", "email", "birthDate");

        // FieldSetMapper
        AnnotationDrivenFieldSetMapper<ChunkUserInput> fieldSetMapper = new AnnotationDrivenFieldSetMapper<>(ChunkUserInput.class);

        // Reader
        FlatFileItemReader<ChunkUserInput> reader = new FlatFileItemReaderBuilder<ChunkUserInput>()
                .name("format3Reader")
                .resource(resource)
                .lineTokenizer(lineTokenizer)
                .fieldSetMapper(fieldSetMapper)
                .build();

        return new StepBuilder("format3ImportStep", jobRepository)
                .<ChunkUserInput, DemoUser>chunk(100, tm2)
                .reader(reader)
                .processor(demoUserInputToDemoUserNoIdProcessor)
                .writer(db2DemoUserWriter)
                .faultTolerant()
                .skipLimit(100)
                .skip(FlatFileParseException.class)
                .skip(NumberFormatException.class)
                .skip(DateTimeParseException.class)
                .build();
    }

    /**
     * Format3 导入 Job (独立执行)
     */
    @Bean
    public Job format3ImportJob(
            JobRepository jobRepository,
            Step format3ImportStep) {

        return new JobBuilder("format3ImportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(format3ImportStep)
                .build();
    }

}
