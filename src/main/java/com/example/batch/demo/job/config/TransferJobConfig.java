package com.example.batch.demo.job.config;

import com.example.batch.demo.job.service.transfer.TransferService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TransferJobConfig {

    @Bean
    public Job transferJob(JobRepository jobRepository, Step produce, Step consume) {
        return new JobBuilder("transferJob", jobRepository)
                .start(produce)
                .next(consume)
                .build();
    }

    @Bean
    public Step produce(JobRepository jobRepository, PlatformTransactionManager transactionManager, TransferService transferService) {
        return new StepBuilder("produce", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    transferService.step1Produce(contribution, chunkContext);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step consume(JobRepository jobRepository, PlatformTransactionManager transactionManager, TransferService transferService) {
        return new StepBuilder("consume", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    transferService.step2Consume(contribution, chunkContext);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
