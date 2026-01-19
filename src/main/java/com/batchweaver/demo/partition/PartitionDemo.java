package com.batchweaver.demo.partition;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 分区并行Demo
 * <p>
 * 使用Partitioner将大文件分区处理
 */
@Configuration
public class PartitionDemo {

    /**
     * 分区Job
     * <p>
     * 将文件分成4个分区并行处理
     */
    @Bean
    public Job partitionJob(JobRepository jobRepository,
                             Step partitionStep) {
        return new JobBuilder("partitionJob", jobRepository)
            .start(partitionStep)
            .build();
    }

    @Bean
    public Step partitionStep(JobRepository jobRepository,
                               Step workerStep) {
        return new StepBuilder("partitionStep", jobRepository)
            .partitioner("workerStep", filePartitioner())
            .step(workerStep)
            .gridSize(4)  // 4个分区
            .taskExecutor(new SimpleAsyncTaskExecutor())  // 并行执行
            .build();
    }

    @Bean
    public Partitioner filePartitioner() {
        return gridSize -> {
            Map<String, ExecutionContext> partitions = new HashMap<>();

            // 假设文件有10000行，分成4个分区
            int totalLines = 10000;
            int linesPerPartition = totalLines / gridSize;

            for (int i = 0; i < gridSize; i++) {
                ExecutionContext context = new ExecutionContext();
                context.putInt("partitionNumber", i);
                // 分区范围：[startLine, endLine) 半开区间（不包含 endLine）
                context.putInt("startLine", i * linesPerPartition);
                // 最后一个分区处理所有剩余行
                int endLine = (i == gridSize - 1) ? totalLines : (i + 1) * linesPerPartition;
                context.putInt("endLine", endLine);
                partitions.put("partition" + i, context);
            }

            return partitions;
        };
    }

    @Bean
    @StepScope
    public Step workerStep(JobRepository jobRepository,
                            PlatformTransactionManager transactionManager,
                            @Value("#{stepExecutionContext['partitionNumber']}") Integer partitionNumber,
                            @Value("#{stepExecutionContext['startLine']}") Integer startLine,
                            @Value("#{stepExecutionContext['endLine']}") Integer endLine) {

        return new StepBuilder("workerStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                System.out.println(String.format("Partition %d: Processing lines %d to %d",
                    partitionNumber, startLine, endLine));
                Thread.sleep(2000);  // 模拟处理
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }
}
