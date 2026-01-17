package com.example.batch.demo.job.config;

import com.example.batch.core.execution.BatchJob;
import com.example.batch.demo.job.service.failurerecovery.FailureRecoveryDemoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 失败恢复演示 Job 配置
 * 
 * 设计目标：验证多数据源场景下的事务回滚和断点续传
 * 
 * Step 流程：
 * 1. db1PrepareStep (db1)：幂等插入 5 条记录 (ID:1001-1005)
 * 2. db2UpdateStep (db2)：普通插入 5 条 PROCESSING 记录
 * 3. db3RiskyStep (db3)：插入后可模拟失败，验证回滚
 * 4. db4CompleteStep (db4)：插入最终 COMPLETED 记录
 * 
 * 事务验证：
 * - Step3 失败时：db3 业务数据回滚，元数据记录 FAILED
 * - RESUME 时：db1/db2 跳过（已完成），db3 重新执行
 */
@Slf4j
@Configuration
@BatchJob(name = "failureRecoveryDemoJob", steps = {"db1PrepareStep", "db2UpdateStep", "db3RiskyStep", "db4CompleteStep"})
public class FailureRecoveryDemoJobConfig {

    @Bean
    public Job failureRecoveryDemoJob(JobRepository jobRepository,
                                      Step db1PrepareStep,
                                      Step db2UpdateStep,
                                      Step db3RiskyStep,
                                      Step db4CompleteStep) {
        return new JobBuilder("failureRecoveryDemoJob", jobRepository)
                .start(db1PrepareStep)
                .next(db2UpdateStep)
                .next(db3RiskyStep)
                .next(db4CompleteStep)
                .build();
    }

    /**
     * Step 1: DB1 准备（幂等操作）
     * 数据源：db1 (BatchWeaverDB)
     * 业务：使用 MERGE 幂等插入 5 条记录 (ID:1001-1005)
     */
    @Bean
    public Step db1PrepareStep(JobRepository jobRepository,
                               @Qualifier("tm1") PlatformTransactionManager tm1,
                               FailureRecoveryDemoService service) {
        return new StepBuilder("db1PrepareStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 1: DB1 准备（幂等） ====================");
                    service.prepareDb1Data();
                    return RepeatStatus.FINISHED;
                }, tm1)  // 使用 tm1
                .build();
    }

    /**
     * Step 2: DB2 更新
     * 数据源：db2 (DB2_Business)
     * 业务：普通插入 5 条 PROCESSING 记录
     */
    @Bean
    public Step db2UpdateStep(JobRepository jobRepository,
                              @Qualifier("tm2") PlatformTransactionManager tm2,
                              FailureRecoveryDemoService service) {
        return new StepBuilder("db2UpdateStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 2: DB2 更新 ====================");
                    service.updateDb2Data();
                    return RepeatStatus.FINISHED;
                }, tm2)  // 使用 tm2
                .build();
    }

    /**
     * Step 3: DB3 风险操作（失败注入点）
     * 数据源：db3 (DB3_Business)
     * 业务：插入后可模拟失败
     * 失败模拟：simulateFail=step3
     */
    @Bean
    public Step db3RiskyStep(JobRepository jobRepository,
                             @Qualifier("tm3") PlatformTransactionManager tm3,
                             FailureRecoveryDemoService service) {
        return new StepBuilder("db3RiskyStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 3: DB3 风险操作 ====================");

                    String simulateFail = (String) chunkContext.getStepContext()
                            .getJobParameters().get("simulateFail");
                    boolean shouldFail = "step3".equalsIgnoreCase(simulateFail);

                    service.riskyDb3Operation(shouldFail);
                    return RepeatStatus.FINISHED;
                }, tm3)  // 使用 tm3
                .build();
    }

    /**
     * Step 4: DB4 完成
     * 数据源：db4 (DB4_Business)
     * 业务：插入最终 COMPLETED 记录
     */
    @Bean
    public Step db4CompleteStep(JobRepository jobRepository,
                                @Qualifier("tm4") PlatformTransactionManager tm4,
                                FailureRecoveryDemoService service) {
        return new StepBuilder("db4CompleteStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 4: DB4 完成 ====================");
                    service.completeDb4Data();
                    return RepeatStatus.FINISHED;
                }, tm4)  // 使用 tm4
                .build();
    }
}
