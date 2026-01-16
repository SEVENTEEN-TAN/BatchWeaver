package com.example.batch.demo.job.config;

import com.example.batch.demo.job.service.multids.Db1BusinessService;
import com.example.batch.demo.job.service.multids.Db2BusinessService;
import com.example.batch.demo.job.service.multids.Db3BusinessService;
import com.example.batch.demo.job.service.multids.Db4BusinessService;
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
 * 多数据源批处理作业配置
 *
 * 作业流程：
 * 1. db1Step：初始化 DB1 数据（元数据 + 业务数据）
 * 2. db2Step：处理 DB2 业务（用户激活）
 * 3. db3Step：处理 DB3 业务（发票对账，可模拟失败）
 * 4. db4Step：处理 DB4 业务（分析数据推送）
 *
 * 关键设计：
 * - 所有 Step 使用 businessTransactionManager（基于 FlexDataSource）
 * - Service 层通过 @UseDataSource 切换数据源
 * - JobRepository 使用独立的 batchTransactionManager（元数据隔离）
 */
@Slf4j
@Configuration
public class MultiDataSourceJobConfig {

    /**
     * 多数据源演示作业
     *
     * 4 个 Step 顺序执行，每个 Step 操作独立的数据源
     */
    @Bean
    public Job multiDataSourceJob(JobRepository jobRepository,
                                  Step db1Step,
                                  Step db2Step,
                                  Step db3Step,
                                  Step db4Step) {
        return new JobBuilder("multiDataSourceJob", jobRepository)
                .start(db1Step)
                .next(db2Step)
                .next(db3Step)
                .next(db4Step)
                .build();
    }

    /**
     * Step 1: 处理 DB1 数据
     * 数据源：DB1 (BatchWeaverDB)
     * 业务：初始化用户数据
     */
    @Bean
    public Step db1Step(JobRepository jobRepository,
                        @Qualifier("businessTransactionManager") PlatformTransactionManager tx,
                        Db1BusinessService service) {
        return new StepBuilder("db1Step", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 1: DB1 数据处理 ====================");
                    service.processDb1Data();
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }

    /**
     * Step 2: 处理 DB2 数据
     * 数据源：DB2 (DB2_Business)
     * 业务：用户激活
     */
    @Bean
    public Step db2Step(JobRepository jobRepository,
                        @Qualifier("businessTransactionManager") PlatformTransactionManager tx,
                        Db2BusinessService service) {
        return new StepBuilder("db2Step", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 2: DB2 数据处理 ====================");
                    service.processDb2Data();
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }

    /**
     * Step 3: 处理 DB3 数据（失败注入点）
     * 数据源：DB3 (DB3_Business)
     * 业务：发票对账
     *
     * 失败模拟：
     * - 通过 JobParameter "simulateFail" 控制是否抛出异常
     * - 失败时，DB3 事务回滚，但元数据已记录 FAILED 状态
     * - 重新运行时，从此 Step 继续执行
     */
    @Bean
    public Step db3Step(JobRepository jobRepository,
                        @Qualifier("businessTransactionManager") PlatformTransactionManager tx,
                        Db3BusinessService service) {
        return new StepBuilder("db3Step", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 3: DB3 数据处理（可模拟失败） ====================");

                    // 从 JobParameters 获取失败标志
                    Boolean simulateFail = (Boolean) chunkContext.getStepContext()
                            .getJobParameters()
                            .get("simulateFail");

                    boolean shouldFail = simulateFail != null && simulateFail;

                    // 执行业务逻辑
                    service.processDb3Data(shouldFail);

                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }

    /**
     * Step 4: 处理 DB4 数据
     * 数据源：DB4 (DB4_Business)
     * 业务：分析数据推送
     */
    @Bean
    public Step db4Step(JobRepository jobRepository,
                        @Qualifier("businessTransactionManager") PlatformTransactionManager tx,
                        Db4BusinessService service) {
        return new StepBuilder("db4Step", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 4: DB4 数据处理 ====================");
                    service.processDb4Data();
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }
}
