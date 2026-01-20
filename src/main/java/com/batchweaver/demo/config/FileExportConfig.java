package com.batchweaver.demo.config;

import com.batchweaver.batch.entity.DemoUser;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Job4: 文件导出测试配置
 * <p>
 * 测试目的：验证数据从数据库导出到文件的功能
 * 支持2种格式：
 * - Format1: yyyyMMdd + 纯数字 Footer
 * - Format2: MMddyyyy + R前缀 Footer
 */
@Configuration
public class FileExportConfig {

    @Value("${batch.weaver.demo.output-path}data/output/")
    private String outputPath;

    @Value("${batch.weaver.demo.chunk-size:1000}")
    private int chunkSize;

    // =============================================================
    // Format1: yyyyMMdd + 纯数字 Footer
    // =============================================================

    /**
     * Format1 导出 Job
     * <p>
     * 输出：yyyyMMdd + 数据行 + count
     */
    @Bean
    public Job format1ExportJob(
            JobRepository jobRepository,
            @Qualifier("tm2") PlatformTransactionManager tm2,
            @Qualifier("dataSource2") DataSource dataSource2) throws Exception {

        // Reader
        JdbcPagingItemReader<DemoUser> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource2);
        reader.setPageSize(chunkSize);
        reader.setRowMapper(new BeanPropertyRowMapper<>(DemoUser.class));
        reader.setQueryProvider(format1QueryProvider(dataSource2));
        reader.setName("format1ExportReader");
        reader.afterPropertiesSet();  // 初始化 reader

        // Writer with Header and Footer
        java.util.concurrent.atomic.AtomicLong writeCount = new java.util.concurrent.atomic.AtomicLong(0);

        FlatFileItemWriter<DemoUser> writer = new FlatFileItemWriterBuilder<DemoUser>()
                .name("format1ExportWriter")
                .resource(new FileSystemResource(outputPath + "format1_export.txt"))
                .delimited()
                .delimiter(",")
                .names("id", "name", "email", "birthDate")
                .headerCallback(headerWriter -> {
                    // 纯日期格式，不带 "HEADER:" 前缀
                    headerWriter.write(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
                })
                .footerCallback(footerWriter -> {
                    // 写入记录数
                    footerWriter.write(String.valueOf(writeCount.get()));
                })
                .build();

        // 包装 writer 以计数
        ItemWriter<DemoUser> countingWriter = items -> {
            writer.write(items);
            writeCount.addAndGet(items.size());
        };

        // Step
        Step step = new StepBuilder("format1ExportStep", jobRepository)
                .<DemoUser, DemoUser>chunk(chunkSize, tm2)
                .reader(reader)
                .writer(countingWriter)
                .build();

        return new JobBuilder("format1ExportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step)
                .build();
    }

    /**
     * Format1 查询提供器
     */
    @Bean
    public PagingQueryProvider format1QueryProvider(@Qualifier("dataSource2") DataSource dataSource2) throws Exception {
        SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
        factory.setDataSource(dataSource2);
        factory.setSelectClause("id, name, email, birth_date");
        factory.setFromClause("FROM DEMO_USER");
        factory.setSortKey("id");
        return factory.getObject();
    }

    // =============================================================
    // Format2: MMddyyyy + R前缀 Footer
    // =============================================================

    /**
     * Format2 导出 Job
     * <p>
     * 输出：MMddyyyy + 数据行 + R00003
     */
    @Bean
    public Job format2ExportJob(
            JobRepository jobRepository,
            @Qualifier("tm2") PlatformTransactionManager tm2,
            @Qualifier("dataSource2") DataSource dataSource2) throws Exception {

        // Reader
        JdbcPagingItemReader<DemoUser> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource2);
        reader.setPageSize(chunkSize);
        reader.setRowMapper(new BeanPropertyRowMapper<>(DemoUser.class));
        reader.setQueryProvider(format2QueryProvider(dataSource2));
        reader.setName("format2ExportReader");
        reader.afterPropertiesSet();  // 初始化 reader

        // Writer with Header and Footer
        java.util.concurrent.atomic.AtomicLong writeCount = new java.util.concurrent.atomic.AtomicLong(0);

        FlatFileItemWriter<DemoUser> writer = new FlatFileItemWriterBuilder<DemoUser>()
                .name("format2ExportWriter")
                .resource(new FileSystemResource(outputPath + "format2_export.txt"))
                .delimited()
                .delimiter(",")
                .names("id", "name", "email", "birthDate")
                .headerCallback(headerWriter -> {
                    // 纯日期格式，不带 "HEADER:" 前缀
                    headerWriter.write(LocalDate.now().format(DateTimeFormatter.ofPattern("MMddyyyy")));
                })
                .footerCallback(footerWriter -> {
                    // 写入记录数，带 R 前缀
                    footerWriter.write("R" + String.format("%05d", writeCount.get()));
                })
                .build();

        // 包装 writer 以计数
        ItemWriter<DemoUser> countingWriter = items -> {
            writer.write(items);
            writeCount.addAndGet(items.size());
        };

        // Step
        Step step = new StepBuilder("format2ExportStep", jobRepository)
                .<DemoUser, DemoUser>chunk(chunkSize, tm2)
                .reader(reader)
                .writer(countingWriter)
                .build();

        return new JobBuilder("format2ExportJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step)
                .build();
    }

    /**
     * Format2 查询提供器
     */
    @Bean
    public PagingQueryProvider format2QueryProvider(@Qualifier("dataSource2") DataSource dataSource2) throws Exception {
        SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
        factory.setDataSource(dataSource2);
        factory.setSelectClause("id, name, email, birth_date");
        factory.setFromClause("FROM DEMO_USER");
        factory.setSortKey("id");
        return factory.getObject();
    }
}
