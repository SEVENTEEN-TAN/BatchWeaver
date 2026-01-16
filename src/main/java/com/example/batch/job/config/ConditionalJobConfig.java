package com.example.batch.job.config;

import com.example.batch.core.execution.BatchJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@BatchJob(name = "conditionalJob", steps = {"step1", "step2", "step3"}, conditionalFlow = true)
public class ConditionalJobConfig {

    @Bean
    public Job conditionalJob(JobRepository jobRepository, Step step1, Step step2, Step step3) {
        return new JobBuilder("conditionalJob", jobRepository)
                .start(step1)
                    .on("FAILED").to(step3) // If Step 1 fails, go to Step 3
                .from(step1)
                    .on("*").to(step2)      // Otherwise, go to Step 2
                .end()
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("step1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("Executing Step 1...");
                    // Simulate failure based on parameter "fail=true"
                    String failParam = (String) chunkContext.getStepContext().getJobParameters().get("fail");
                    if ("true".equalsIgnoreCase(failParam)) {
                        log.error("Step 1 Failed!");
                        throw new RuntimeException("Step 1 intentional failure");
                    }
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step step2(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("step2", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("Executing Step 2 (Success Path)...");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step step3(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("step3", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("Executing Step 3 (Failure Path)...");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
