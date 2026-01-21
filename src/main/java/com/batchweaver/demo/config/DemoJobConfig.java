package com.batchweaver.demo.config;

import com.batchweaver.demo.shared.entity.DemoUser;
import com.batchweaver.demo.shared.entity.DemoUserInput;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    /**
     * 关键配置：Step 使用 tm2（业务事务管理器）
     *
     * @param jobRepository JobRepository（使用 tm1Meta 管理元数据）
     * @param tm2           业务事务管理器（管理 db2 的业务数据）
     */
    @Bean
    public Step importFileStep(JobRepository jobRepository,
                               PlatformTransactionManager tm2,
                               ItemReader<DemoUserInput> demoUsersFileReader,
                               ItemProcessor<DemoUserInput, DemoUser> demoUserInputToDemoUserCopyIdProcessor,
                               ItemWriter<DemoUser> db2DemoUserWriter) {
        return new StepBuilder("importFileStep", jobRepository)
                .<DemoUserInput, DemoUser>chunk(100, tm2)  // chunk() 的泛型: <输入类型, 输出类型>
                .reader(demoUsersFileReader)
                .processor(demoUserInputToDemoUserCopyIdProcessor)
                .writer(db2DemoUserWriter)
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
