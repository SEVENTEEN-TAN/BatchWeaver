package com.example.batch.demo.job.config;

import com.example.batch.core.execution.BatchJob;
import com.example.batch.demo.job.service.multids.MultiDsDemoService;
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
 * 多数据源演示 Job 配置
 * 
 * 设计目标：每个 Step 操作不同数据源，覆盖 4 种执行模式
 * 
 * Step 流程：
 * 1. db1InitStep (db1)：清空 + 插入 10 条 PENDING 数据
 * 2. db2ProcessStep (db2)：清空 + 插入 8 条 PROCESSING 数据
 * 3. db3TransformStep (db3)：清空 + 插入 6 条 TRANSFORMED 数据
 * 4. db4FinalizeStep (db4)：清空 + 插入 4 条 COMPLETED 数据
 * 
 * 失败模拟：通过 simulateFail 参数控制（step2/step3/step4）
 * 
 * 测试覆盖：
 * - STANDARD：正常顺序执行 4 个数据源
 * - RESUME：某 Step 失败后从该 Step 续传
 * - SKIP_FAIL：某 Step 失败跳过，继续后续 Step
 * - ISOLATED：独立执行指定 Step
 */
@Slf4j
@Configuration
@BatchJob(name = "multiDsDemoJob", steps = {"db1InitStep", "db2ProcessStep", "db3TransformStep", "db4FinalizeStep"})
public class MultiDsDemoJobConfig {

    @Bean
    public Job multiDsDemoJob(JobRepository jobRepository,
                              Step db1InitStep,
                              Step db2ProcessStep,
                              Step db3TransformStep,
                              Step db4FinalizeStep) {
        return new JobBuilder("multiDsDemoJob", jobRepository)
                .start(db1InitStep)
                .next(db2ProcessStep)
                .next(db3TransformStep)
                .next(db4FinalizeStep)
                .build();
    }

    /**
     * Step 1: DB1 初始化
     * 数据源：db1 (BatchWeaverDB)
     * 业务：清空表 + 插入 10 条 PENDING 数据
     */
    @Bean
    public Step db1InitStep(JobRepository jobRepository,
                            @Qualifier("businessTransactionManager") PlatformTransactionManager tx,
                            MultiDsDemoService service) {
        return new StepBuilder("db1InitStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 1: DB1 初始化 ====================");
                    service.initDb1Data();
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }

    /**
     * Step 2: DB2 处理
     * 数据源：db2 (DB2_Business)
     * 业务：清空表 + 插入 8 条 PROCESSING 数据
     * 失败模拟：simulateFail=step2
     */
    @Bean
    public Step db2ProcessStep(JobRepository jobRepository,
                               @Qualifier("businessTransactionManager") PlatformTransactionManager tx,
                               MultiDsDemoService service) {
        return new StepBuilder("db2ProcessStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 2: DB2 处理 ====================");
                    
                    String simulateFail = (String) chunkContext.getStepContext()
                            .getJobParameters().get("simulateFail");
                    boolean shouldFail = "step2".equalsIgnoreCase(simulateFail);
                    
                    service.processDb2Data(shouldFail);
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }

    /**
     * Step 3: DB3 转换
     * 数据源：db3 (DB3_Business)
     * 业务：清空表 + 插入 6 条 TRANSFORMED 数据
     * 失败模拟：simulateFail=step3
     */
    @Bean
    public Step db3TransformStep(JobRepository jobRepository,
                                 @Qualifier("businessTransactionManager") PlatformTransactionManager tx,
                                 MultiDsDemoService service) {
        return new StepBuilder("db3TransformStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 3: DB3 转换 ====================");
                    
                    String simulateFail = (String) chunkContext.getStepContext()
                            .getJobParameters().get("simulateFail");
                    boolean shouldFail = "step3".equalsIgnoreCase(simulateFail);
                    
                    service.transformDb3Data(shouldFail);
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }

    /**
     * Step 4: DB4 完成
     * 数据源：db4 (DB4_Business)
     * 业务：清空表 + 插入 4 条 COMPLETED 数据
     * 失败模拟：simulateFail=step4
     */
    @Bean
    public Step db4FinalizeStep(JobRepository jobRepository,
                                @Qualifier("businessTransactionManager") PlatformTransactionManager tx,
                                MultiDsDemoService service) {
        return new StepBuilder("db4FinalizeStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 4: DB4 完成 ====================");
                    
                    String simulateFail = (String) chunkContext.getStepContext()
                            .getJobParameters().get("simulateFail");
                    boolean shouldFail = "step4".equalsIgnoreCase(simulateFail);
                    
                    service.finalizeDb4Data(shouldFail);
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }
}
