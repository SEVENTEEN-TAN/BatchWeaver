package com.batchweaver.batch.config;

import com.batchweaver.batch.entity.DemoUser;
import com.batchweaver.batch.service.Db2BusinessService;
import com.batchweaver.core.processor.DataCleansingProcessor;
import com.batchweaver.core.reader.AnnotationDrivenFieldSetMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.BindException;

/**
 * 示例 Job 配置 - 文件导入到 db2
 * <p>
 * 核心配置：展示事务隔离机制
 * - Step 使用 tm2（业务事务管理器）
 * - JobRepository 使用 tm1Meta（元数据专用事务管理器）
 * - 失败时：业务事务回滚，元数据事务提交 FAILED 状态
 */
@Configuration
public class DemoJobConfig {

    @Value("${batch.chunk-size:100}")
    private int chunkSize;

    /**
     * FlatFileItemReader - 文件读取器
     */
    @Bean
    public FlatFileItemReader<DemoUser> demoUserReader() {
        return new FlatFileItemReaderBuilder<DemoUser>()
                .name("demoUserReader")
                .resource(new FileSystemResource("data/input/demo_users.txt"))
                .delimited()
                .delimiter("|")
                .names("id", "name", "email", "birthDate")  // 列名（仅用于 FieldSet）
                .fieldSetMapper(new AnnotationDrivenFieldSetMapper<>(DemoUser.class))
                .linesToSkip(1)  // 跳过首行（H|...）
                .build();
    }

    /**
     * ItemProcessor - 数据处理器
     */
    @Bean
    public ItemProcessor<DemoUser, DemoUser> demoUserProcessor() {
        return new DataCleansingProcessor<>();
    }

    /**
     * ItemWriter - 数据写入器（调用 Service 层）
     */
    @Bean
    public ItemWriter<DemoUser> demoUserWriter(Db2BusinessService db2BusinessService) {
        return items -> db2BusinessService.batchInsertUsers(new java.util.ArrayList<>(items.getItems()));
    }

    /**
     * 关键配置：Step 使用 tm2（业务事务管理器）
     *
     * @param jobRepository JobRepository（使用 tm1Meta 管理元数据）
     * @param tm2           业务事务管理器（管理 db2 的业务数据）
     */
    @Bean
    public Step importFileStep(JobRepository jobRepository,
                               @Qualifier("tm2") PlatformTransactionManager tm2,
                               FlatFileItemReader<DemoUser> reader,
                               ItemProcessor<DemoUser, DemoUser> processor,
                               ItemWriter<DemoUser> writer) {
        return new StepBuilder("importFileStep", jobRepository)
                .<DemoUser, DemoUser>chunk(chunkSize, tm2)  // chunk() 的第二个参数就是事务管理器
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(10)
                .skip(FlatFileParseException.class)      // 文件解析异常
                .skip(BindException.class)               // 字段绑定异常
                .skip(IllegalArgumentException.class)     // 非法参数异常
                .build();
    }

    /**
     * Job 配置
     */
    @Bean
    public Job demoJob(JobRepository jobRepository, Step importFileStep) {
        return new JobBuilder("demoJob", jobRepository)
                .start(importFileStep)
                .build();
    }
}
