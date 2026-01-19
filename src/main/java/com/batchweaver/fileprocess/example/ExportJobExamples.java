package com.batchweaver.fileprocess.example;

import com.batchweaver.batch.entity.DemoUser;
import com.batchweaver.fileprocess.core.FileExportJobTemplate;
import com.batchweaver.fileprocess.reader.AnnotationRowMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.SqlServerPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/**
 * 导出Job示例
 * <p>
 * 展示3种导出格式
 */
@Configuration
public class ExportJobExamples {

    private final FileExportJobTemplate template = new FileExportJobTemplate();

    /**
     * 示例1：yyyyMMdd + 纯数字记录数
     */
    @Bean
    public Job format1ExportJob(JobRepository jobRepository,
                                  @Qualifier("tm2") PlatformTransactionManager tm2,
                                  @Qualifier("dataSource2") DataSource dataSource2) throws Exception {

        // Reader
        JdbcPagingItemReader<DemoUser> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource2);
        reader.setPageSize(1000);
        reader.setRowMapper(new AnnotationRowMapper<>(DemoUser.class));

        SqlServerPagingQueryProvider queryProvider = new SqlServerPagingQueryProvider();
        queryProvider.setSelectClause("SELECT id AS userId, name AS userName, email, birth_date AS birthDate");
        queryProvider.setFromClause("FROM DEMO_USER");
        queryProvider.setSortKeys(new HashMap<String, Order>() {{
            put("id", Order.ASCENDING);
        }});
        reader.setQueryProvider(queryProvider);
        reader.afterPropertiesSet();

        // 构建Job定义
        FileExportJobTemplate.FileExportJobDefinition<DemoUser> definition =
            FileExportJobTemplate.FileExportJobDefinition.<DemoUser>builder()
                .jobName("format1ExportJob")
                .stepName("format1ExportStep")
                .jobRepository(jobRepository)
                .transactionManager(tm2)
                .reader(reader)
                .resource(new FileSystemResource("data/output/format1_export.txt"))
                .entityClass(DemoUser.class)
                .delimiter(",")
                // 头生成：yyyyMMdd
                .headerGenerator(date -> date.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
                // 尾生成：纯数字
                .footerGenerator(String::valueOf)
                .chunkSize(1000)
                .build();

        return template.buildJob(definition);
    }

    /**
     * 示例2：MMddyyyy + R前缀记录数
     */
    @Bean
    public Job format2ExportJob(JobRepository jobRepository,
                                  @Qualifier("tm2") PlatformTransactionManager tm2,
                                  @Qualifier("dataSource2") DataSource dataSource2) throws Exception {

        JdbcPagingItemReader<DemoUser> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource2);
        reader.setPageSize(1000);
        reader.setRowMapper(new AnnotationRowMapper<>(DemoUser.class));

        SqlServerPagingQueryProvider queryProvider = new SqlServerPagingQueryProvider();
        queryProvider.setSelectClause("SELECT id AS userId, name AS userName, email, birth_date AS birthDate");
        queryProvider.setFromClause("FROM DEMO_USER");
        queryProvider.setSortKeys(new HashMap<String, Order>() {{
            put("id", Order.ASCENDING);
        }});
        reader.setQueryProvider(queryProvider);
        reader.afterPropertiesSet();

        FileExportJobTemplate.FileExportJobDefinition<DemoUser> definition =
            FileExportJobTemplate.FileExportJobDefinition.<DemoUser>builder()
                .jobName("format2ExportJob")
                .stepName("format2ExportStep")
                .jobRepository(jobRepository)
                .transactionManager(tm2)
                .reader(reader)
                .resource(new FileSystemResource("data/output/format2_export.txt"))
                .entityClass(DemoUser.class)
                .delimiter(",")
                // 头生成：MMddyyyy
                .headerGenerator(date -> date.format(DateTimeFormatter.ofPattern("MMddyyyy")))
                // 尾生成：R前缀
                .footerGenerator(count -> String.format("R%05d", count))
                .chunkSize(1000)
                .build();

        return template.buildJob(definition);
    }
}
