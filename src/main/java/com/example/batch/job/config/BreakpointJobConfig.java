package com.example.batch.job.config;

import com.example.batch.job.service.breakpoint.BreakpointService;
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
public class BreakpointJobConfig {

    @Bean
    public Job breakpointJob(JobRepository jobRepository, Step prepareStep, Step processStep, Step cleanupStep) {
        return new JobBuilder("breakpointJob", jobRepository)
                .start(prepareStep)
                .next(processStep)
                .next(cleanupStep)
                .build();
    }

    @Bean
    public Step prepareStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, BreakpointService breakpointService) {
        return new StepBuilder("prepareStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    breakpointService.prepare();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step processStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, BreakpointService breakpointService) {
        return new StepBuilder("processStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    breakpointService.processWithPotentialFailure();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step cleanupStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, BreakpointService breakpointService) {
        return new StepBuilder("cleanupStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    breakpointService.cleanup();
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
