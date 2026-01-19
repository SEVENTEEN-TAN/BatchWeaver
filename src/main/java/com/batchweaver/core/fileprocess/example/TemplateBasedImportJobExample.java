package com.batchweaver.core.fileprocess.example;

import com.batchweaver.batch.entity.DemoUser;
import com.batchweaver.core.processor.DataCleansingProcessor;
import com.batchweaver.core.reader.AnnotationDrivenFieldSetMapper;
import com.batchweaver.core.fileprocess.core.FileImportJobTemplate;
import com.batchweaver.core.fileprocess.model.FooterInfo;
import com.batchweaver.core.fileprocess.model.HeaderInfo;
import com.batchweaver.core.fileprocess.writer.JdbcBatchItemWriterFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 使用FileImportJobTemplate的完整示例
 * <p>
 * 展示如何使用模板快速构建导入Job
 */
@Configuration
public class TemplateBasedImportJobExample {

    private final FileImportJobTemplate template = new FileImportJobTemplate();

    /**
     * 使用模板构建导入Job
     * <p>
     * 文件格式：yyyyMMdd + 纯数字记录数
     */
    @Bean
    public Job templateImportJob(JobRepository jobRepository,
                                  @Qualifier("tm2") PlatformTransactionManager tm2,
                                  @Qualifier("dataSource2") DataSource dataSource2) {

        FileSystemResource resource = new FileSystemResource("data/input/template_users.txt");

        // Reader
        FlatFileItemReader<DemoUser> reader = new FlatFileItemReaderBuilder<DemoUser>()
            .name("templateReader")
            .resource(resource)
            .delimited()
            .delimiter(",")
            .names("name", "age", "email", "birthDate")
            .fieldSetMapper(new AnnotationDrivenFieldSetMapper<>(DemoUser.class))
            .linesToSkip(1)  // 跳过头行
            .build();

        // Processor
        DataCleansingProcessor<DemoUser> processor = new DataCleansingProcessor<>();

        // Writer（使用批量写入优化）
        String sql = "INSERT INTO DEMO_USER (id, name, email, birth_date) " +
                     "VALUES (:id, :name, :email, :birthDate)";
        var writer = JdbcBatchItemWriterFactory.create(dataSource2, sql, DemoUser.class);

        // 构建Job定义
        FileImportJobTemplate.FileImportJobDefinition definition =
            FileImportJobTemplate.FileImportJobDefinition.builder()
                .jobName("templateImportJob")
                .stepName("templateImportStep")
                .jobRepository(jobRepository)
                .transactionManager(tm2)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .resource(resource)
                // 头尾处理（Lambda）
                .headerParser(line -> new HeaderInfo(
                    LocalDate.parse(line, DateTimeFormatter.ofPattern("yyyyMMdd"))))
                .headerValidator(header -> {
                    if (!header.getDate().equals(LocalDate.now())) {
                        throw new IllegalStateException("Date mismatch");
                    }
                })
                .footerParser(line -> new FooterInfo(Long.parseLong(line.trim())))
                .footerValidator((footer, actual) -> {
                    if (footer.getCount() != actual) {
                        throw new IllegalStateException(
                            String.format("Count mismatch: expected=%d, actual=%d",
                                footer.getCount(), actual));
                    }
                })
                // 性能配置
                .chunkSize(1000)
                // 错误处理
                .skipLimit(100)
                .retryLimit(3)
                .build();

        // 使用模板构建Job
        return template.buildJob(definition);
    }

    /**
     * 简化版：无头尾校验
     */
    @Bean
    public Job simpleTemplateImportJob(JobRepository jobRepository,
                                        @Qualifier("tm2") PlatformTransactionManager tm2,
                                        @Qualifier("dataSource2") DataSource dataSource2) {

        FileSystemResource resource = new FileSystemResource("data/input/simple_users.txt");

        FlatFileItemReader<DemoUser> reader = new FlatFileItemReaderBuilder<DemoUser>()
            .name("simpleReader")
            .resource(resource)
            .delimited()
            .delimiter(",")
            .names("name", "age", "email", "birthDate")
            .fieldSetMapper(new AnnotationDrivenFieldSetMapper<>(DemoUser.class))
            .build();

        String sql = "INSERT INTO DEMO_USER (id, name, email, birth_date) " +
                     "VALUES (:id, :name, :email, :birthDate)";
        var writer = JdbcBatchItemWriterFactory.create(dataSource2, sql, DemoUser.class);

        FileImportJobTemplate.FileImportJobDefinition definition =
            FileImportJobTemplate.FileImportJobDefinition.builder()
                .jobName("simpleTemplateImportJob")
                .stepName("simpleTemplateImportStep")
                .jobRepository(jobRepository)
                .transactionManager(tm2)
                .reader(reader)
                .writer(writer)
                .chunkSize(1000)
                .skipLimit(100)
                .build();

        return template.buildJob(definition);
    }
}
