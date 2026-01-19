package com.batchweaver.demo.multithread;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.IntStream;

/**
 * 多线程并行Demo
 * <p>
 * 使用TaskExecutor在单个Step内并行处理Chunk
 */
@Configuration
public class MultiThreadDemo {

    /**
     * 多线程Job
     * <p>
     * 使用10个线程并行处理1000条数据
     */
    @Bean
    public Job multiThreadJob(JobRepository jobRepository,
                               Step multiThreadStep) {
        return new JobBuilder("multiThreadJob", jobRepository)
            .start(multiThreadStep)
            .build();
    }

    @Bean
    public Step multiThreadStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager) {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(10);  // 最多10个线程

        return new StepBuilder("multiThreadStep", jobRepository)
            .<Integer, Integer>chunk(50, transactionManager)
            .reader(multiThreadReader())
            .processor(multiThreadProcessor())
            .writer(multiThreadWriter())
            .taskExecutor(taskExecutor)
            .build();
    }

    @Bean
    @StepScope
    @SuppressWarnings("unchecked")
    public SynchronizedItemStreamReader<Integer> multiThreadReader() {
        List<Integer> items = IntStream.rangeClosed(1, 1000)
            .boxed()
            .collect(Collectors.toList());

        ListItemReader<Integer> listReader = new ListItemReader<>(items);

        SynchronizedItemStreamReader<Integer> synchronizedReader = new SynchronizedItemStreamReader<>();
        synchronizedReader.setDelegate((org.springframework.batch.item.ItemStreamReader<Integer>) listReader);

        return synchronizedReader;
    }

    @Bean
    public ItemProcessor<Integer, Integer> multiThreadProcessor() {
        return item -> {
            System.out.println(String.format("Thread %s processing item %d",
                Thread.currentThread().getName(), item));
            Thread.sleep(100);  // 模拟处理
            return item * 2;
        };
    }

    @Bean
    public ItemWriter<Integer> multiThreadWriter() {
        return items -> {
            System.out.println(String.format("Thread %s writing %d items",
                Thread.currentThread().getName(), items.size()));
        };
    }
}
