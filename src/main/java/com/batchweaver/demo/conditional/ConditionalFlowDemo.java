package com.batchweaver.demo.conditional;

import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 条件流路由Demo
 * <p>
 * 使用JobExecutionDecider根据条件路由到不同Step
 */
@Configuration
public class ConditionalFlowDemo {

    /**
     * 条件流路由Job
     * <p>
     * 根据文件类型路由到不同的处理Step
     */
    @Bean
    public Job conditionalFlowJob(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager) {

        return new JobBuilder("conditionalFlowJob", jobRepository)
            .start(validateStep(jobRepository, transactionManager))
            .next(fileTypeDecider())
                .on("USER").to(processUserStep(jobRepository, transactionManager))
                .from(fileTypeDecider()).on("ORDER").to(processOrderStep(jobRepository, transactionManager))
                .from(fileTypeDecider()).on("PRODUCT").to(processProductStep(jobRepository, transactionManager))
                .from(fileTypeDecider()).on("*").fail()
            .end()
            .build();
    }

    @Bean
    public Step validateStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("validateStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                System.out.println("Validating file...");
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

    @Bean
    public JobExecutionDecider fileTypeDecider() {
        return (jobExecution, stepExecution) -> {
            String fileType = jobExecution.getJobParameters().getString("fileType");
            System.out.println("File type: " + fileType);

            if (fileType == null) {
                return new FlowExecutionStatus("UNKNOWN");
            }

            return new FlowExecutionStatus(fileType.toUpperCase());
        };
    }

    @Bean
    public Step processUserStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("processUserStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                System.out.println("Processing USER file...");
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

    @Bean
    public Step processOrderStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("processOrderStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                System.out.println("Processing ORDER file...");
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

    @Bean
    public Step processProductStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("processProductStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                System.out.println("Processing PRODUCT file...");
                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }
}
