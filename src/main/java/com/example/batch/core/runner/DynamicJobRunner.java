package com.example.batch.core.runner;

import com.example.batch.core.execution.DynamicJobBuilderService;
import com.example.batch.core.logging.ExecutionContextLogger;
import com.example.batch.core.execution.ExecutionMode;
import com.example.batch.core.execution.ExecutionModeValidator;
import com.example.batch.core.execution.JobMetadataRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 动态 Job 运行器
 * 统一入口，支持 4 种执行模式
 */
@Slf4j
@Component
public class DynamicJobRunner implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final ApplicationContext applicationContext;
    private final DynamicJobBuilderService dynamicJobBuilderService;
    private final JobMetadataRegistry jobMetadataRegistry;
    private final ExecutionModeValidator executionModeValidator;

    public DynamicJobRunner(JobLauncher jobLauncher,
                            ApplicationContext applicationContext,
                            DynamicJobBuilderService dynamicJobBuilderService,
                            JobMetadataRegistry jobMetadataRegistry,
                            ExecutionModeValidator executionModeValidator) {
        this.jobLauncher = jobLauncher;
        this.applicationContext = applicationContext;
        this.dynamicJobBuilderService = dynamicJobBuilderService;
        this.jobMetadataRegistry = jobMetadataRegistry;
        this.executionModeValidator = executionModeValidator;
    }

    @Override
    public void run(String... args) throws Exception {
        String jobName = null;
        Properties params = new Properties();

        // 解析命令行参数
        for (String arg : args) {
            if (arg.startsWith("jobName=")) {
                jobName = arg.split("=", 2)[1];
            } else if (arg.contains("=")) {
                String[] parts = arg.split("=", 2);
                if (parts.length >= 2) {
                    params.put(parts[0], parts[1]);
                }
            }
        }

        if (jobName == null) {
            log.info("No jobName provided in arguments. Skipping CLI job execution.");
            String[] jobNames = applicationContext.getBeanNamesForType(Job.class);
            log.info("Available jobs: {}", Arrays.toString(jobNames));
            log.info("Registered jobs with @BatchJob: {}", jobMetadataRegistry.getRegisteredJobNames());
            return;
        }

        // 解析执行模式和关键参数
        ExecutionMode mode = ExecutionMode.from(params.getProperty("_mode"));
        String targetSteps = params.getProperty("_target_steps");
        Long id = params.containsKey("id") && isNumeric(params.getProperty("id")) 
            ? Long.parseLong(params.getProperty("id")) 
            : null;

        // [P2 优化] 先校验执行模式，再生成参数
        try {
            executionModeValidator.validate(jobName, mode, id, targetSteps);
        } catch (ExecutionModeValidator.ExecutionModeValidationException e) {
            log.error("Validation failed: {}", e.getMessage());
            return;
        }

        // 校验通过后，构建 JobParameters
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        for (String key : params.stringPropertyNames()) {
            String value = params.getProperty(key);
            if ("id".equals(key) && isNumeric(value)) {
                paramsBuilder.addLong(key, Long.parseLong(value));
            } else {
                paramsBuilder.addString(key, value);
            }
        }

        // 确保 _mode 参数被记录（用于后续校验历史模式）
        if (params.getProperty("_mode") == null) {
            paramsBuilder.addString("_mode", "STANDARD");
        }

        // 为无 ID 的执行生成唯一标识（确保可以创建新的 JobInstance）
        // RESUME 模式必须携带 ID，因此不需要生成 _timestamp
        if (id == null && mode != ExecutionMode.RESUME) {
            paramsBuilder.addLong("_timestamp", System.currentTimeMillis());
        }

        JobParameters jobParameters = paramsBuilder.toJobParameters();

        // 构建并执行 Job
        Job job = buildJob(jobName, mode, jobParameters);
        if (job == null) {
            return;
        }

        log.info("Starting job: {} with mode: {}", jobName, mode);
        executeJob(job, jobParameters);
    }

    /**
     * 构建 Job
     */
    private Job buildJob(String jobName, ExecutionMode mode, JobParameters jobParameters) {
        try {
            // 对于 STANDARD 模式，如果 Job 未注册，直接使用原生 Bean
            if (mode == ExecutionMode.STANDARD && !jobMetadataRegistry.isRegistered(jobName)) {
                return applicationContext.getBean(jobName, Job.class);
            }

            // 使用动态构建服务
            List<JobExecutionListener> listeners = getListeners();
            return dynamicJobBuilderService.buildJob(jobName, mode, jobParameters, listeners);
            
        } catch (Exception e) {
            log.error("Failed to build job '{}': {}", jobName, e.getMessage());
            return null;
        }
    }

    /**
     * 获取 Job 监听器
     */
    private List<JobExecutionListener> getListeners() {
        try {
            ExecutionContextLogger executionLogger = applicationContext.getBean(ExecutionContextLogger.class);
            return List.of(executionLogger);
        } catch (Exception e) {
            log.warn("ExecutionContextLogger not found, proceeding without it");
            return List.of();
        }
    }

    /**
     * 执行 Job
     */
    private void executeJob(Job job, JobParameters jobParameters) {
        try {
            JobExecution execution = jobLauncher.run(job, jobParameters);
            log.info("JobExecution started id={} status={}", execution.getId(), execution.getStatus());
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("Job already completed for parameters, not re-executing. " +
                "Use _mode=ISOLATED with _target_steps to re-run specific steps.");
        } catch (JobExecutionAlreadyRunningException e) {
            log.info("Job is already running, skip duplicate execution.");
        } catch (Exception e) {
            log.error("Job execution failed", e);
        }
    }

    private boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }
}
