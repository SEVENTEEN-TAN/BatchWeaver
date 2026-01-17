package com.example.batch.demo.job.config;

import com.example.batch.core.execution.BatchJob;
import com.example.batch.demo.job.service.skipfail.SkipFailDemoService;
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
 * 容错执行演示 Job 配置
 * 
 * 设计目标：验证 SKIP_FAIL 模式下多数据源的部分失败容忍
 * 
 * Step 流程：
 * 1. db1FetchStep (db1)：插入源数据
 * 2. db2ParseStep (db2)：解析并插入
 * 3. db3ValidateStep (db3)：校验并插入
 * 4. db4PersistStep (db4)：最终持久化
 * 
 * 容错验证：
 * - 任意 Step 失败，后续 Step 继续执行
 * - 失败的 Step 对应数据源数据回滚
 * - Job 最终状态 COMPLETED，但 ExitStatus 包含跳过信息
 * 
 * 失败模拟：simulateFail=step1/step2/step3/step4 或逗号分隔多个
 */
@Slf4j
@Configuration
@BatchJob(name = "skipFailDemoJob", steps = {"db1FetchStep", "db2ParseStep", "db3ValidateStep", "db4PersistStep"})
public class SkipFailDemoJobConfig {

    @Bean
    public Job skipFailDemoJob(JobRepository jobRepository,
                               Step db1FetchStep,
                               Step db2ParseStep,
                               Step db3ValidateStep,
                               Step db4PersistStep) {
        return new JobBuilder("skipFailDemoJob", jobRepository)
                .start(db1FetchStep)
                .next(db2ParseStep)
                .next(db3ValidateStep)
                .next(db4PersistStep)
                .build();
    }

    /**
     * Step 1: DB1 获取数据
     * 数据源：db1 (BatchWeaverDB)
     * 业务：插入源数据
     * 失败模拟：simulateFail 包含 step1
     */
    @Bean
    public Step db1FetchStep(JobRepository jobRepository,
                             @Qualifier("tm1") PlatformTransactionManager tm1,
                             SkipFailDemoService service) {
        return new StepBuilder("db1FetchStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 1: DB1 获取数据 ====================");

                    String simulateFail = (String) chunkContext.getStepContext()
                            .getJobParameters().get("simulateFail");
                    boolean shouldFail = shouldFailStep(simulateFail, "step1");

                    service.fetchDb1Data(shouldFail);
                    return RepeatStatus.FINISHED;
                }, tm1)  // 使用 tm1
                .build();
    }

    /**
     * Step 2: DB2 解析数据
     * 数据源：db2 (DB2_Business)
     * 业务：解析并插入
     * 失败模拟：simulateFail 包含 step2
     */
    @Bean
    public Step db2ParseStep(JobRepository jobRepository,
                             @Qualifier("tm2") PlatformTransactionManager tm2,
                             SkipFailDemoService service) {
        return new StepBuilder("db2ParseStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 2: DB2 解析数据 ====================");

                    String simulateFail = (String) chunkContext.getStepContext()
                            .getJobParameters().get("simulateFail");
                    boolean shouldFail = shouldFailStep(simulateFail, "step2");

                    service.parseDb2Data(shouldFail);
                    return RepeatStatus.FINISHED;
                }, tm2)  // 使用 tm2
                .build();
    }

    /**
     * Step 3: DB3 校验数据
     * 数据源：db3 (DB3_Business)
     * 业务：校验并插入
     * 失败模拟：simulateFail 包含 step3
     */
    @Bean
    public Step db3ValidateStep(JobRepository jobRepository,
                                @Qualifier("tm3") PlatformTransactionManager tm3,
                                SkipFailDemoService service) {
        return new StepBuilder("db3ValidateStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 3: DB3 校验数据 ====================");

                    String simulateFail = (String) chunkContext.getStepContext()
                            .getJobParameters().get("simulateFail");
                    boolean shouldFail = shouldFailStep(simulateFail, "step3");

                    service.validateDb3Data(shouldFail);
                    return RepeatStatus.FINISHED;
                }, tm3)  // 使用 tm3
                .build();
    }

    /**
     * Step 4: DB4 持久化
     * 数据源：db4 (DB4_Business)
     * 业务：最终持久化
     * 失败模拟：simulateFail 包含 step4
     */
    @Bean
    public Step db4PersistStep(JobRepository jobRepository,
                               @Qualifier("tm4") PlatformTransactionManager tm4,
                               SkipFailDemoService service) {
        return new StepBuilder("db4PersistStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 4: DB4 持久化 ====================");

                    String simulateFail = (String) chunkContext.getStepContext()
                            .getJobParameters().get("simulateFail");
                    boolean shouldFail = shouldFailStep(simulateFail, "step4");

                    service.persistDb4Data(shouldFail);
                    return RepeatStatus.FINISHED;
                }, tm4)  // 使用 tm4
                .build();
    }

    /**
     * 判断指定 Step 是否应该失败
     * 支持逗号分隔的多个 Step 名称，如：step2,step3
     */
    private boolean shouldFailStep(String simulateFail, String stepName) {
        if (simulateFail == null || simulateFail.trim().isEmpty()) {
            return false;
        }
        String[] steps = simulateFail.toLowerCase().split(",");
        for (String step : steps) {
            if (step.trim().equals(stepName.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
