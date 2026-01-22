package com.batchweaver.demo.config;

import com.batchweaver.demo.shared.entity.DemoUser;
import com.batchweaver.demo.shared.service.Db2BusinessService;
import com.batchweaver.demo.shared.service.Db3BusinessService;
import com.batchweaver.demo.shared.service.Db4BusinessService;
import com.batchweaver.demo.shared.entity.DemoUserInput;
import com.batchweaver.demo.shared.service.MockMailSender;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Job5: 复杂工作流测试配置
 * <p>
 * 测试目的：验证多步骤、条件分支、邮件通知的复杂工作流
 * <p>
 * 工作流：
 * Step1: Import (导入数据)
 * Step2: Sync to DB3 (DB2 → DB3)
 * Step3: Sync to DB4 (DB2 → DB4)
 * Step4: Decision Check (决策)
 * Step5: 发送失败邮件
 * Step6: 发送成功邮件
 * Step7: Export (导出数据)
 */
@Configuration
public class ComplexWorkflowConfig {
    /**
     * 复杂工作流 Job
     */
    @Bean
    public Job complexWorkflowJob(
            JobRepository jobRepository,
            Step step1Import,
            Step step2SyncToDb3,
            Step step3SyncToDb4,
            JobExecutionDecider decisionStep4,
            Step step5FailureMail,
            Step step6SuccessMail,
            Step step7Export) {

        return new JobBuilder("complexWorkflowJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(step1Import)
                .next(step2SyncToDb3)
                .next(step3SyncToDb4)
                .next(decisionStep4)
                .on("FAILED").to(step5FailureMail)
                .from(decisionStep4)
                .on("COMPLETED_WITH_SKIPS").to(step6SuccessMail).next(step7Export)
                .from(decisionStep4)
                .on("COMPLETED").to(step6SuccessMail).next(step7Export)
                .end()
                .build();
    }

    // =============================================================
    // Step1: Import (导入数据)
    // =============================================================

    /**
     * Step1: 读取 data/input/workflow_users.txt -> DB2
     */
    @Bean
    public Step step1Import(
            JobRepository jobRepository,
            ItemReader<DemoUserInput> workflowFileReader,
            ItemProcessor<DemoUserInput, DemoUser> demoUserInputToDemoUserNoIdProcessor,
            ItemWriter<DemoUser> db2DemoUserWriter,
            PlatformTransactionManager tm2
    ) {
        return new StepBuilder("step1Import", jobRepository)
                .<DemoUserInput, DemoUser>chunk(100, tm2)
                .reader(workflowFileReader)
                .processor(demoUserInputToDemoUserNoIdProcessor)
                .writer(db2DemoUserWriter)
                .faultTolerant()
                .skipLimit(100)
                .skip(FlatFileParseException.class)
                .skip(NumberFormatException.class)
                .skip(DateTimeParseException.class)
                .build();
    }

    // =============================================================
    // Step2: Sync to DB3 (DB2 → DB3)
    // =============================================================

    /**
     * Step2: DB2->同步数据到 DB3
     * <p>
     * 使用 tm3 事务管理器
     */
    @Bean
    public Step step2SyncToDb3(
            JobRepository jobRepository,
            PlatformTransactionManager tm3,
            Db2BusinessService db2BusinessService,
            Db3BusinessService db3BusinessService) {

        return new StepBuilder("step2SyncToDb3", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // 从 DB2 读取数据
                    List<DemoUser> users = db2BusinessService.getAllUsers();
                    // 同步到 DB3（使用 tm3）
                    db3BusinessService.batchInsertUsers(users);
                    System.out.println("[STEP2] Synced " + users.size() + " users from DB2 to DB3");
                    return RepeatStatus.FINISHED;
                }, tm3)
                .build();
    }

    // =============================================================
    // Step3: Sync to DB4 (DB2 → DB4)
    // =============================================================

    /**
     * Step3: DB2 同步数据到 DB4
     * <p>
     * 使用 tm4 事务管理器
     */
    @Bean
    public Step step3SyncToDb4(
            JobRepository jobRepository,
            @Qualifier("tm4") PlatformTransactionManager tm4,
            Db2BusinessService db2BusinessService,
            Db4BusinessService db4BusinessService) {

        return new StepBuilder("step3SyncToDb4", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // 从 DB2 读取数据
                    List<DemoUser> users = db2BusinessService.getAllUsers();
                    // 同步到 DB4（使用 tm4）
                    db4BusinessService.batchInsertUsers(users);
                    System.out.println("[STEP3] Synced " + users.size() + " users from DB2 to DB4");
                    return RepeatStatus.FINISHED;
                }, tm4)
                .build();
    }

    // =============================================================
    // Step4: Decision Check (决策)
    // =============================================================

    /**
     * 决策器：根据处理结果决定路由
     */
    @Bean
    public JobExecutionDecider decisionStep4() {
        return (jobExecution, stepExecution) -> {
            // 从 jobExecution 中获取 step1Import 的执行信息
            StepExecution step1Execution = jobExecution.getStepExecutions().stream()
                    .filter(se -> "step1Import".equals(se.getStepName()))
                    .findFirst()
                    .orElse(null);

            if (step1Execution == null) {
                return new FlowExecutionStatus("FAILED");
            }

            long skipCount = step1Execution.getSkipCount();

            // 检查状态
            if (step1Execution.getStatus() == BatchStatus.FAILED) {
                return new FlowExecutionStatus("FAILED");
            } else if (skipCount > 0) {
                return new FlowExecutionStatus("COMPLETED_WITH_SKIPS");
            } else {
                return new FlowExecutionStatus("COMPLETED");
            }
        };
    }

    // =============================================================
    // Step5: 发送失败邮件
    // =============================================================

    /**
     * Step5: 发送失败邮件
     */
    @Bean
    public Step step5FailureMail(
            JobRepository jobRepository,
            PlatformTransactionManager tm2,
            MockMailSender mockMailSender) {

        return new StepBuilder("step5FailureMail", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    mockMailSender.sendFailure(
                            "Job5 Failed - Complex Workflow",
                            "The complex workflow job has failed. Please check the logs for details."
                    );
                    return RepeatStatus.FINISHED;
                }, tm2)
                .build();
    }

    // =============================================================
    // Step6: 发送成功邮件
    // =============================================================

    /**
     * Step6: 发送成功邮件（支持部分成功）
     */
    @Bean
    public Step step6SuccessMail(
            JobRepository jobRepository,
            PlatformTransactionManager tm2,
            MockMailSender mockMailSender) {

        return new StepBuilder("step6SuccessMail", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // 检查是否有 skip
                    long skipCount = chunkContext.getStepContext().getStepExecution().getSkipCount();

                    if (skipCount > 0) {
                        mockMailSender.sendPartial(
                                "Job5 Partial Success - Complex Workflow",
                                "The job completed with " + skipCount + " records skipped."
                        );
                    } else {
                        mockMailSender.sendSuccess(
                                "Job5 Success - Complex Workflow",
                                "The complex workflow job completed successfully."
                        );
                    }
                    return RepeatStatus.FINISHED;
                }, tm2)
                .build();
    }

    // =============================================================
    // Step7: Export (导出数据)
    // =============================================================

    /**
     * Step7: 导出更新后的数据
     */
    @Bean
    public Step step7Export(
            JobRepository jobRepository,
            @Qualifier("tm2") PlatformTransactionManager tm2,
            @Qualifier("dataSource2") DataSource dataSource2,
            MockMailSender mockMailSender) throws Exception {

        // Reader
        JdbcPagingItemReader<DemoUser> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource2);
        reader.setPageSize(100);
        reader.setRowMapper(new BeanPropertyRowMapper<>(DemoUser.class));
        reader.setQueryProvider(resultExportQueryProvider(dataSource2));
        reader.setName("resultExportReader");
        reader.afterPropertiesSet();  // 初始化 reader

        // Writer
        FlatFileItemWriter<DemoUser> writer = new FlatFileItemWriterBuilder<DemoUser>()
                .name("resultExportWriter")
                .resource(new FileSystemResource("data/output/result_export.txt"))
                .delimited()
                .delimiter(",")
                .names("id", "name", "email", "birthDate")
                .headerCallback(headerWriter -> {
                    headerWriter.write("EXPORT_DATE:" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + System.lineSeparator());
                })
                .build();

        return new StepBuilder("step7Export", jobRepository)
                .<DemoUser, DemoUser>chunk(100, tm2)
                .reader(reader)
                .writer(writer)
                .stream(writer)  // 注册 writer 到 stream，让 Spring Batch 管理其生命周期
                .listener(new ExportCompletionListener(mockMailSender))
                .build();
    }

    /**
     * 结果导出查询提供器
     */
    @Bean
    public PagingQueryProvider resultExportQueryProvider(DataSource dataSource2) throws Exception {
        SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
        factory.setDataSource(dataSource2);
        factory.setSelectClause("id, name, email, birth_date");
        factory.setFromClause("FROM DEMO_USER");
        factory.setSortKey("id");
        return factory.getObject();
    }

    /**
     * 导出完成监听器
     */
    public static class ExportCompletionListener implements StepExecutionListener {
        private final MockMailSender mockMailSender;

        public ExportCompletionListener(MockMailSender mockMailSender) {
            this.mockMailSender = mockMailSender;
        }

        @Override
        public org.springframework.batch.core.ExitStatus afterStep(org.springframework.batch.core.StepExecution stepExecution) {
            if (stepExecution.getStatus().name().equals("COMPLETED")) {
                mockMailSender.sendSuccess(
                        "Export Completed - Job5",
                        "Step7 (Export) completed. " + stepExecution.getWriteCount() + " records exported."
                );
            }
            return stepExecution.getExitStatus();
        }
    }
}
