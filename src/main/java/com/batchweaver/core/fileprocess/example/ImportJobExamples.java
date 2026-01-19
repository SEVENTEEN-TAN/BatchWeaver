package com.batchweaver.core.fileprocess.example;

import com.batchweaver.batch.entity.DemoUser;
import com.batchweaver.batch.service.Db2BusinessService;
import com.batchweaver.core.processor.DataCleansingProcessor;
import com.batchweaver.core.reader.AnnotationDrivenFieldSetMapper;
import com.batchweaver.core.fileprocess.function.*;
import com.batchweaver.core.fileprocess.listener.HeaderFooterListener;
import com.batchweaver.core.fileprocess.model.FooterInfo;
import com.batchweaver.core.fileprocess.model.HeaderInfo;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 导入Job示例 - 展示3种头尾格式
 * <p>
 * 格式1：yyyyMMdd + 纯数字记录数
 * 格式2：MMddyyyy + R前缀记录数
 * 格式3：无头尾
 */
@Configuration
public class ImportJobExamples {

    /**
     * 示例1：yyyyMMdd + 纯数字记录数
     * <p>
     * 文件格式：
     * 20260119
     * 张三,25,zhangsan@example.com,1990-01-15
     * 李四,30,lisi@example.com,1985-06-20
     * 3
     */
    @Bean
    public Job format1ImportJob(JobRepository jobRepository,
                                  @Qualifier("tm2") PlatformTransactionManager tm2,
                                  Db2BusinessService db2BusinessService) {

        FileSystemResource resource = new FileSystemResource("data/input/format1_users.txt");

        // 头解析：yyyyMMdd
        HeaderParser headerParser = line -> {
            LocalDate date = LocalDate.parse(line, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return new HeaderInfo(date);
        };

        // 头校验：日期必须是今天
        HeaderValidator headerValidator = header -> {
            if (!header.getDate().equals(LocalDate.now())) {
                throw new IllegalStateException(
                    String.format("Header date mismatch: expected=%s, actual=%s",
                        LocalDate.now(), header.getDate()));
            }
        };

        // 尾解析：纯数字
        FooterParser footerParser = line -> {
            long count = Long.parseLong(line.trim());
            return new FooterInfo(count);
        };

        // 尾校验：记录数必须匹配
        FooterValidator footerValidator = (footer, actual) -> {
            if (footer.getCount() != actual) {
                throw new IllegalStateException(
                    String.format("Footer count mismatch: expected=%d, actual=%d",
                        footer.getCount(), actual));
            }
        };

        FlatFileItemReader<DemoUser> reader = new FlatFileItemReaderBuilder<DemoUser>()
            .name("format1Reader")
            .resource(resource)
            .delimited()
            .delimiter(",")
            .names("name", "age", "email", "birthDate")
            .fieldSetMapper(new AnnotationDrivenFieldSetMapper<>(DemoUser.class))
            .linesToSkip(1)  // 跳过头行
            .build();

        ItemProcessor<DemoUser, DemoUser> processor = new DataCleansingProcessor<>();

        ItemWriter<DemoUser> writer = items ->
            db2BusinessService.batchInsertUsers(new java.util.ArrayList<>(items.getItems()));

        Step step = new StepBuilder("format1ImportStep", jobRepository)
            .<DemoUser, DemoUser>chunk(1000, tm2)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .listener(new HeaderFooterListener(resource, headerParser, headerValidator,
                footerParser, footerValidator))
            .build();

        return new JobBuilder("format1ImportJob", jobRepository)
            .start(step)
            .build();
    }

    /**
     * 示例2：MMddyyyy + R前缀记录数
     * <p>
     * 文件格式：
     * 01192026
     * 张三,25,zhangsan@example.com,1990-01-15
     * 李四,30,lisi@example.com,1985-06-20
     * R00003
     */
    @Bean
    public Job format2ImportJob(JobRepository jobRepository,
                                  @Qualifier("tm2") PlatformTransactionManager tm2,
                                  Db2BusinessService db2BusinessService) {

        FileSystemResource resource = new FileSystemResource("data/input/format2_users.txt");

        // 头解析：MMddyyyy
        HeaderParser headerParser = line ->
            new HeaderInfo(LocalDate.parse(line, DateTimeFormatter.ofPattern("MMddyyyy")));

        // 头校验：可选
        HeaderValidator headerValidator = header -> {
            // 可以添加自定义校验逻辑
        };

        // 尾解析：R前缀
        FooterParser footerParser = line -> {
            String countStr = line.substring(1); // 去掉R
            long count = Long.parseLong(countStr);
            return new FooterInfo(count);
        };

        // 尾校验：记录数必须匹配
        FooterValidator footerValidator = (footer, actual) -> {
            if (footer.getCount() != actual) {
                throw new IllegalStateException(
                    String.format("Footer count mismatch: expected=%d, actual=%d",
                        footer.getCount(), actual));
            }
        };

        FlatFileItemReader<DemoUser> reader = new FlatFileItemReaderBuilder<DemoUser>()
            .name("format2Reader")
            .resource(resource)
            .delimited()
            .delimiter(",")
            .names("name", "age", "email", "birthDate")
            .fieldSetMapper(new AnnotationDrivenFieldSetMapper<>(DemoUser.class))
            .linesToSkip(1)
            .build();

        ItemProcessor<DemoUser, DemoUser> processor = new DataCleansingProcessor<>();

        ItemWriter<DemoUser> writer = items ->
            db2BusinessService.batchInsertUsers(new java.util.ArrayList<>(items.getItems()));

        Step step = new StepBuilder("format2ImportStep", jobRepository)
            .<DemoUser, DemoUser>chunk(1000, tm2)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .listener(new HeaderFooterListener(resource, headerParser, headerValidator,
                footerParser, footerValidator))
            .build();

        return new JobBuilder("format2ImportJob", jobRepository)
            .start(step)
            .build();
    }

    /**
     * 示例3：无头尾
     * <p>
     * 文件格式：
     * 张三,25,zhangsan@example.com,1990-01-15
     * 李四,30,lisi@example.com,1985-06-20
     * 王五,28,wangwu@example.com,1987-03-10
     */
    @Bean
    public Job format3ImportJob(JobRepository jobRepository,
                                  @Qualifier("tm2") PlatformTransactionManager tm2,
                                  Db2BusinessService db2BusinessService) {

        FileSystemResource resource = new FileSystemResource("data/input/format3_users.txt");

        FlatFileItemReader<DemoUser> reader = new FlatFileItemReaderBuilder<DemoUser>()
            .name("format3Reader")
            .resource(resource)
            .delimited()
            .delimiter(",")
            .names("name", "age", "email", "birthDate")
            .fieldSetMapper(new AnnotationDrivenFieldSetMapper<>(DemoUser.class))
            .build();

        ItemProcessor<DemoUser, DemoUser> processor = new DataCleansingProcessor<>();

        ItemWriter<DemoUser> writer = items ->
            db2BusinessService.batchInsertUsers(new java.util.ArrayList<>(items.getItems()));

        Step step = new StepBuilder("format3ImportStep", jobRepository)
            .<DemoUser, DemoUser>chunk(1000, tm2)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            // 无头尾监听器
            .build();

        return new JobBuilder("format3ImportJob", jobRepository)
            .start(step)
            .build();
    }
}
