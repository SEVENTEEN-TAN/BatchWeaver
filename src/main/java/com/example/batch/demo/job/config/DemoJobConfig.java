package com.example.batch.demo.job.config;

import com.example.batch.core.execution.BatchJob;
import com.example.batch.demo.job.service.demo.DemoService;
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
@BatchJob(name = "demoJob", steps = {"importStep", "updateStep", "exportStep"})
public class DemoJobConfig {

    @Bean
    public Job demoJob(JobRepository jobRepository, Step importStep, Step updateStep, Step exportStep) {
        return new JobBuilder("demoJob", jobRepository)
                .start(importStep)
                .next(updateStep)
                .next(exportStep)
                .build();
    }

    @Bean
    public Step importStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, DemoService demoService) {
        return new StepBuilder("importStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    demoService.importData();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step updateStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, DemoService demoService) {
        return new StepBuilder("updateStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    demoService.updateData();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step exportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, DemoService demoService) {
        return new StepBuilder("exportStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    demoService.exportData();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
