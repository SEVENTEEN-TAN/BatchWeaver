package com.batchweaver.demo.jobs;

import com.batchweaver.core.fileprocess.listener.FooterValidationListener;
import com.batchweaver.demo.entity.DemoUser;
import com.batchweaver.demo.entity.DemoUserInput;
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
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.format.DateTimeParseException;

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
            PlatformTransactionManager tm2,
            ItemReader<DemoUserInput> demoUsersFileReader,
            ItemProcessor<DemoUserInput, DemoUser> demoUserInputToDemoUserCopyIdProcessor,
            ItemWriter<DemoUser> db2DemoUserWriter,
            FooterValidationListener footerValidationListener) {

        return new StepBuilder("conditionalImportStep", jobRepository)
                .<DemoUserInput, DemoUser>chunk(100, tm2)
                .reader(demoUsersFileReader)
                .processor(demoUserInputToDemoUserCopyIdProcessor)
                .writer(db2DemoUserWriter)
                .listener(footerValidationListener)
                .faultTolerant()
                .skipLimit(10)
                .skip(FlatFileParseException.class)
                .skip(NumberFormatException.class)
                .skip(DateTimeParseException.class)
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
            PlatformTransactionManager tm2) {

        return new StepBuilder("successStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("========================================");
                    System.out.println("[SUCCESS BRANCH] Job executed successfully");
                    System.out.println("========================================");
                    return RepeatStatus.FINISHED;
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
                    return RepeatStatus.FINISHED;
                }, tm2)
                .build();
    }

}
