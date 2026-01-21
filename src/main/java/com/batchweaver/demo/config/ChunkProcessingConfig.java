package com.batchweaver.demo.config;

import com.batchweaver.demo.shared.entity.DemoUser;
import com.batchweaver.demo.shared.entity.DemoUserInput;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
     * Chunk 模式 Job
     */
    @Bean
    public Job chunkProcessingJob(
            JobRepository jobRepository,
            Step chunkProcessingStep) {

        return new JobBuilder("chunkProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(chunkProcessingStep)
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
            ItemReader<DemoUserInput> largeFileReader,
            ItemProcessor<DemoUserInput, DemoUser> demoUserInputToDemoUserNoIdProcessor,
            ItemWriter<DemoUser> db2DemoUserWriter,
            ChunkListener chunkExecutionListener,
            StepExecutionListener StepExecutionListenerImpl
    ) {

        return new StepBuilder("chunkProcessingStep", jobRepository)
                .<DemoUserInput, DemoUser>chunk(100, tm2)
                .reader(largeFileReader)
                .processor(demoUserInputToDemoUserNoIdProcessor)
                .writer(db2DemoUserWriter)
                .faultTolerant()
                .skipLimit(100)
                .skip(org.springframework.batch.item.file.FlatFileParseException.class)
                .skip(java.lang.NumberFormatException.class)
                .skip(java.time.format.DateTimeParseException.class)
                .listener(chunkExecutionListener)
                .listener(StepExecutionListenerImpl)
                .build();
    }
}
