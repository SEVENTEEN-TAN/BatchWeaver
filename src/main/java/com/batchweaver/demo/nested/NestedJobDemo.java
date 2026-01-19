package com.batchweaver.demo.nested;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Job嵌套编排Demo
 * <p>
 * 父Job调度多个子Job
 */
@Configuration
public class NestedJobDemo {

    /**
     * 父Job：数据迁移流程
     * <p>
     * 编排：导入 → 校验 → 导出
     */
    @Bean
    public Job parentMigrationJob(JobRepository jobRepository,
                                   Job childImportJob,
                                   Job childValidateJob,
                                   Job childExportJob) {

        return new JobBuilder("parentMigrationJob", jobRepository)
            .start(createJobStep(jobRepository, childImportJob, "importJobStep"))
            .next(createJobStep(jobRepository, childValidateJob, "validateJobStep"))
            .next(createJobStep(jobRepository, childExportJob, "exportJobStep"))
            .build();
    }

    private Step createJobStep(JobRepository jobRepository, Job job, String stepName) {
        return new StepBuilder(stepName, jobRepository)
            .job(job)
            .build();
    }

    /**
     * 子Job1：导入
     */
    @Bean
    public Job childImportJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder("childImportJob", jobRepository)
            .start(new StepBuilder("importStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("Child Job: Importing data...");
                    Thread.sleep(1000);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build())
            .build();
    }

    /**
     * 子Job2：校验
     */
    @Bean
    public Job childValidateJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder("childValidateJob", jobRepository)
            .start(new StepBuilder("validateStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("Child Job: Validating data...");
                    Thread.sleep(500);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build())
            .build();
    }

    /**
     * 子Job3：导出
     */
    @Bean
    public Job childExportJob(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder("childExportJob", jobRepository)
            .start(new StepBuilder("exportStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("Child Job: Exporting data...");
                    Thread.sleep(1000);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build())
            .build();
    }
}
