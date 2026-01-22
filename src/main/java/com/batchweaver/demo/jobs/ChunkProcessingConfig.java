package com.batchweaver.demo.jobs;

import com.batchweaver.core.fileprocess.reader.HeaderFooterAwareReader;
import com.batchweaver.core.transaction.TransactionLogger;
import com.batchweaver.demo.entity.ChunkUserInput;
import com.batchweaver.demo.entity.DemoUser;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Job2: 批处理模式测试配置（Decider 模式重构版）
 * <p>
 * 测试目的：验证基于 Chunk 的批处理模式
 * 文件格式：yyyyMMdd + 数据行 + count
 * <p>
 * 设计模式：使用 Decider 进行条件决策，职责分离更清晰
 * <p>
 * 工作流：
 * chunkProcessingStep → recordCountDecider
 * ├─ VALID → Job COMPLETED
 * └─ INVALID → cleanupStep → Step COMPLETED, Job FAILED
 *
 * @author BatchWeaver Team
 * @since 1.0.0
 */
@Configuration
public class ChunkProcessingConfig {

    /**
     * Chunk 模式 Job (使用 Decider 模式)
     */
    @Bean
    public Job chunkProcessingJob(
            JobRepository jobRepository,
            Step chunkProcessingStep,
            JobExecutionDecider recordCountDecider,
            Step cleanupStep) {

        return new JobBuilder("chunkProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(chunkProcessingStep)
                .next(recordCountDecider)
                .on("INVALID").to(cleanupStep)
                .from(recordCountDecider).on("VALID").stop()
                .end()
                .build();
    }

    /**
     * 读取 data/input/large_users.txt -> DB2
     * Chunk 处理 Step
     */
    @Bean
    public Step chunkProcessingStep(
            JobRepository jobRepository,
            @Qualifier("tm2") PlatformTransactionManager tm2,
            HeaderFooterAwareReader<ChunkUserInput> largeFileReader,
            ItemProcessor<ChunkUserInput, DemoUser> demoUserInputToDemoUserNoIdProcessor,
            ItemWriter<DemoUser> db2DemoUserWriter,
            ChunkListener chunkExecutionListener,
            StepExecutionListener StepExecutionListenerImpl
    ) {

        return new StepBuilder("chunkProcessingStep", jobRepository)
                .<ChunkUserInput, DemoUser>chunk(10, tm2)
                .reader(largeFileReader)
                .processor(demoUserInputToDemoUserNoIdProcessor)
                .writer(db2DemoUserWriter)
                .faultTolerant()
                .listener(chunkExecutionListener)
                .listener(StepExecutionListenerImpl)
                .listener(largeFileReader)
                .build();
    }

    /**
     * 记录数校验决策器
     * <p>
     * 职责：检查数据库记录数是否与 Footer 声明一致
     * 返回：VALID（一致）或 INVALID（不一致）
     */
    @Bean
    public JobExecutionDecider recordCountDecider(
            @Qualifier("jdbcTemplate2") JdbcTemplate jdbcTemplate2) {

        return (jobExecution, stepExecution) -> {
            ExecutionContext jobContext = jobExecution.getExecutionContext();

            // 获取 Footer 声明的记录数
            Long declaredCount = jobContext.getLong("declaredRecordCount", -1L);

            if (declaredCount == -1L) {
                System.out.println("[DECIDER] No footer information found, skipping validation");
                return new FlowExecutionStatus("VALID");
            }

            // 查询数据库实际记录数
            Long actualCount = jdbcTemplate2.queryForObject(
                    "SELECT COUNT(*) FROM DEMO_USER", Long.class);

            System.out.println("[DECIDER] Declared count: " + declaredCount +
                    ", Actual DB count: " + actualCount);

            if (declaredCount.equals(actualCount)) {
                System.out.println("[DECIDER] ✅ Validation PASSED");
                return new FlowExecutionStatus("VALID");
            } else {
                System.out.println("[DECIDER] ❌ Validation FAILED - mismatch detected");
                System.out.println("[DECIDER] Will proceed to cleanup step");
                return new FlowExecutionStatus("INVALID");
            }
        };
    }

    /**
     * 清理步骤：删除所有数据
     * <p>
     * 职责：删除 DEMO_USER 表的所有数据
     * 事务：使用默认 REQUIRED 传播行为
     * <p>
     * 重要：
     * 1. 此 Step 正常完成，不抛异常
     * 2. Step 完成后通过 StepExecutionListener 将 Job 状态标记为 FAILED
     */
    @Bean
    public Step cleanupStep(
            JobRepository jobRepository,
            @Qualifier("jdbcTemplate2") JdbcTemplate jdbcTemplate2,
            @Qualifier("tm2") PlatformTransactionManager tm2) {

        Tasklet cleanupTasklet = (contribution, chunkContext) -> {
            StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
            JobExecution jobExecution = stepExecution.getJobExecution();
            ExecutionContext jobContext = jobExecution.getExecutionContext();

            Long declaredCount = jobContext.getLong("declaredRecordCount", -1L);

            System.out.println("[CLEANUP] Starting data cleanup...");
            System.out.println("[CLEANUP] Declared: " + declaredCount);

            // 使用默认 REQUIRED 事务（由 Step 管理）
            TransactionLogger.TransactionContext txContext = TransactionLogger.logTransactionStart(
                    "cleanupStep-cleanup", new DefaultTransactionDefinition());
            TransactionLogger.registerTransactionSynchronization(txContext);

            // 删除所有数据
            TransactionLogger.logSqlExecution("DELETE FROM DEMO_USER");
            jdbcTemplate2.execute("DELETE FROM DEMO_USER");
            System.out.println("[CLEANUP] Deleted all rows from DEMO_USER");

            // 设置 Step 的退出描述（记录详细信息）
            String exitDescription = String.format(
                    "Cleanup completed: Declared=%d, Action=DELETE FROM DEMO_USER",
                    declaredCount);
            stepExecution.setExitStatus(ExitStatus.COMPLETED.addExitDescription(exitDescription));

            // 将校验失败信息存储到 JobExecutionContext
            jobContext.putString("validation.failure.reason",
                    String.format("Footer declared %d records, cleanup executed.",
                            declaredCount));

            System.out.println("[CLEANUP] ✅ Data cleanup completed successfully");
            System.out.println("[CLEANUP] " + exitDescription);

            // ⚠️ 关键：不抛出异常！让 Step 正常完成
            // Job 的失败状态由 StepExecutionListener 控制
            return RepeatStatus.FINISHED;
        };

        return new StepBuilder("cleanupStep", jobRepository)
                .tasklet(cleanupTasklet, tm2)
                .listener(cleanupStepListener())
                .build();
    }

    /**
     * 清理步骤监听器：在 Step 完成后将 Job 标记为 FAILED
     */
    @Bean
    public StepExecutionListener cleanupStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                // 不需要特殊处理
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                // Step 完成后，将 Job 标记为 FAILED
                JobExecution jobExecution = stepExecution.getJobExecution();
                if (jobExecution != null) {
                    jobExecution.setStatus(BatchStatus.FAILED);
                    jobExecution.setExitStatus(ExitStatus.FAILED.addExitDescription(
                            "Validation failed: " +
                            jobExecution.getExecutionContext().getString("validation.failure.reason",
                            "Unknown validation failure")));
                    System.out.println("[CLEANUP-LISTENER] Job status set to FAILED");
                }
                return stepExecution.getExitStatus();
            }
        };
    }
}
