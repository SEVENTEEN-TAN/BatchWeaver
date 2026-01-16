package com.example.batch.job.config;

import com.example.batch.core.execution.BatchJob;
import com.example.batch.core.execution.ExecutionContextLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 高级控制作业配置
 * 支持 4 种执行模式：STANDARD, RESUME, SKIP_FAIL, ISOLATED
 */
@Slf4j
@Configuration
@BatchJob(name = "advancedControlJob", steps = {"advStep1", "advStep2", "advStep3", "advStep4"})
public class AdvancedControlJobConfig {

    /**
     * 执行上下文日志器 Bean
     */
    @Bean
    public ExecutionContextLogger executionLogger() {
        return new ExecutionContextLogger();
    }

    /**
     * advancedControlJob 原生 Job 定义
     * 用于 STANDARD 和 RESUME 模式
     */
    @Bean
    public Job advancedControlJob(JobRepository jobRepository,
                                   Step advStep1, Step advStep2, Step advStep3, Step advStep4,
                                   ExecutionContextLogger executionLogger) {
        return new JobBuilder("advancedControlJob", jobRepository)
            .listener(executionLogger)
            .start(advStep1)
            .next(advStep2)
            .next(advStep3)
            .next(advStep4)
            .build();
    }

    // ========== Step 定义 ==========

    /**
     * Step1：数据准备
     */
    @Bean
    public Step advStep1(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("advStep1", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("[advStep1] Preparing data...");

                // 模拟业务逻辑
                String simulateFail = (String) chunkContext.getStepContext().getJobParameters().get("simulateFail");
                if ("step1".equalsIgnoreCase(simulateFail)) {
                    throw new RuntimeException("Simulated failure in Step1");
                }

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

    /**
     * Step2：数据处理
     */
    @Bean
    public Step advStep2(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("advStep2", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("[advStep2] Processing data...");

                String simulateFail = (String) chunkContext.getStepContext().getJobParameters().get("simulateFail");
                if ("step2".equalsIgnoreCase(simulateFail)) {
                    throw new RuntimeException("Simulated failure in Step2");
                }

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

    /**
     * Step3：数据转换
     */
    @Bean
    public Step advStep3(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("advStep3", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("[advStep3] Transforming data...");

                String simulateFail = (String) chunkContext.getStepContext().getJobParameters().get("simulateFail");
                if ("step3".equalsIgnoreCase(simulateFail)) {
                    throw new RuntimeException("Simulated failure in Step3");
                }

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

    /**
     * Step3 容错版本：支持跳过异常
     */
    @Bean
    public Step advStep3FaultTolerant(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("advStep3FaultTolerant", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("[advStep3] Transforming data (fault-tolerant mode)...");

                String simulateFail = (String) chunkContext.getStepContext().getJobParameters().get("simulateFail");
                if ("step3".equalsIgnoreCase(simulateFail)) {
                    log.warn("[advStep3] Exception caught but marked as SKIPPED");
                    contribution.setExitStatus(org.springframework.batch.core.ExitStatus.COMPLETED
                        .addExitDescription("SKIPPED due to fault tolerance"));
                    return RepeatStatus.FINISHED;
                }

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

    /**
     * Step4：数据输出
     */
    @Bean
    public Step advStep4(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("advStep4", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("[advStep4] Outputting results...");

                String simulateFail = (String) chunkContext.getStepContext().getJobParameters().get("simulateFail");
                if ("step4".equalsIgnoreCase(simulateFail)) {
                    throw new RuntimeException("Simulated failure in Step4");
                }

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }
}
