package com.batchweaver.demo.jobs;

import com.batchweaver.demo.entity.ChunkUserInput;
import com.batchweaver.demo.entity.DemoUser;
import com.batchweaver.demo.service.Db2BusinessService;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Job2: 批处理模式测试配置
 * <p>
 * 测试目的：验证基于 Chunk 的批处理模式
 * 文件格式：yyyyMMdd + 数据行 + count
 */
@Configuration
public class ChunkProcessingConfig {

    /**
     * Chunk 模式 Job (带后置校验)
     */
    @Bean
    public Job chunkProcessingJob(
            JobRepository jobRepository,
            Step chunkProcessingStep,
            Step postValidationStep) {

        return new JobBuilder("chunkProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(chunkProcessingStep)
                .next(postValidationStep)
                .build();
    }

    /**
     * 读取 data/input/large_users.txt -> DB2
     * Chunk 处理 Step
     */
    @Bean
    public Step chunkProcessingStep(
            JobRepository jobRepository,
            PlatformTransactionManager tm2,
            ItemReader<ChunkUserInput> largeFileReader,
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
                .build();
    }

    /**
     * 后置校验步骤：验证数据库记录数与 Footer 声明的数量是否一致
     */
    @Bean
    public Step postValidationStep(
            JobRepository jobRepository,
            PlatformTransactionManager tm2,
            Tasklet postValidationTasklet) {

        return new StepBuilder("postValidationStep", jobRepository)
                .tasklet(postValidationTasklet, tm2)
                .build();
    }

    /**
     * 后置校验 Tasklet
     */
    @Bean
    public Tasklet postValidationTasklet(
            JdbcTemplate jdbcTemplate2,
            Db2BusinessService db2BusinessService) {

        return (contribution, chunkContext) -> {
            StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
            JobExecution jobExecution = stepExecution.getJobExecution();
            ExecutionContext jobContext = jobExecution.getExecutionContext();

            // 获取 Footer 声明的记录数 (使用 HeaderFooterAwareReader 中定义的 key)
            Long declaredCount = jobContext.getLong("declaredRecordCount", -1L);

            if (declaredCount == -1L) {
                // 没有 Footer 信息,跳过校验
                System.out.println("[POST-VALIDATION] No footer information found, skipping validation");
                return RepeatStatus.FINISHED;
            }

            // 查询数据库实际记录数
            Long actualCount = jdbcTemplate2.queryForObject(
                    "SELECT COUNT(*) FROM DEMO_USER", Long.class);

            System.out.println("[POST-VALIDATION] Declared count: " + declaredCount +
                             ", Actual DB count: " + actualCount);

            if (!declaredCount.equals(actualCount)) {
                // 数量不匹配,清理数据并失败
                System.out.println("[POST-VALIDATION] Count mismatch! Rolling back data...");
                jdbcTemplate2.execute("DELETE FROM DEMO_USER");
                System.out.println("[POST-VALIDATION] Data rolled back successfully");

                throw new IllegalStateException(
                    "Post-validation failed: Footer declared " + declaredCount +
                    " records, but database contains " + actualCount + " records. Data has been rolled back.");
            }

            System.out.println("[POST-VALIDATION] Validation passed");
            return RepeatStatus.FINISHED;
        };
    }
}
