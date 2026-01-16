package com.example.batch.core.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 动态 Job 构建服务
 * 根据执行模式动态构建 Job 执行计划
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicJobBuilderService {

    private final JobRepository jobRepository;
    private final ApplicationContext applicationContext;
    private final ExecutionStatusService executionStatusService;
    private final JobMetadataRegistry jobMetadataRegistry;

    /**
     * 根据执行模式构建 Job
     *
     * @param jobName       Job 名称
     * @param mode          执行模式
     * @param jobParameters Job 参数
     * @param listeners     Job 监听器列表
     * @return 构建好的 Job
     * @throws JobExecutionException 如果构建失败
     */
    public Job buildJob(String jobName, ExecutionMode mode, JobParameters jobParameters, 
                        List<JobExecutionListener> listeners) throws JobExecutionException {
        
        JobMetadataRegistry.JobMetadata metadata = jobMetadataRegistry.get(jobName);
        Long id = jobParameters.getLong("id");
        String targetSteps = jobParameters.getString("_target_steps");

        log.info("Building job '{}' with mode={}, id={}", jobName, mode, id);

        return switch (mode) {
            case STANDARD -> buildStandardJob(jobName, metadata, listeners);
            case RESUME -> buildResumeJob(jobName, metadata, id, listeners);
            case SKIP_FAIL -> buildSkipFailJob(jobName, metadata, listeners);
            case ISOLATED -> buildIsolatedJob(jobName, metadata, id, targetSteps, listeners);
        };
    }

    /**
     * 构建 STANDARD 模式 Job
     * 使用原生 Job Bean
     */
    private Job buildStandardJob(String jobName, JobMetadataRegistry.JobMetadata metadata,
                                  List<JobExecutionListener> listeners) throws JobExecutionException {
        try {
            Job originalJob = applicationContext.getBean(jobName, Job.class);
            log.info("STANDARD mode: using original job bean '{}'", jobName);
            return originalJob;
        } catch (Exception e) {
            throw new JobExecutionException("Failed to get job bean: " + jobName, e);
        }
    }

    /**
     * 构建 RESUME 模式 Job
     * 动态构建只包含需要续传的 Step 的 Flow
     */
    private Job buildResumeJob(String jobName, JobMetadataRegistry.JobMetadata metadata,
                                Long id, List<JobExecutionListener> listeners) throws JobExecutionException {

        JobExecution historicalExecution = executionStatusService.getJobExecution(id);
        if (historicalExecution == null) {
            throw new JobExecutionException("No execution found for id=" + id);
        }

        // 获取需要重新执行的 Step 列表
        List<String> allSteps = metadata != null ? metadata.steps() : List.of();
        List<String> stepsToResume = executionStatusService.getStepsToResumeFrom(historicalExecution, allSteps);

        String firstFailed = executionStatusService.getFirstFailedStepName(historicalExecution);
        log.info("RESUME mode: resuming from step '{}', will execute: {}", firstFailed, stepsToResume);

        // 构建只包含续传 Step 的线性 Flow
        Flow flow = buildLinearFlow(jobName, stepsToResume);

        JobBuilder jobBuilder = new JobBuilder(jobName + "_resume", jobRepository);
        for (JobExecutionListener listener : listeners) {
            jobBuilder.listener(listener);
        }

        // 添加历史上下文加载监听器（复用历史执行的业务数据）
        jobBuilder.listener(new ExecutionContextLoaderListener(historicalExecution));
        log.info("RESUME mode: will load ExecutionContext from historical execution id={}", id);

        return jobBuilder
            .start(flow)
            .end()
            .build();
    }

    /**
     * 构建 SKIP_FAIL 模式 Job
     * 构建容错 Flow：任意 Step 失败都继续执行下一个 Step
     * 
     * <p>实现原理：</p>
     * <ul>
     *   <li>使用 Flow 的 on("*") 匹配任意 ExitStatus（包括 FAILED）</li>
     *   <li>显式处理 FAILED 转换到下一个 Step</li>
     *   <li>最终 Job 状态为 COMPLETED（即使有 Step 失败）</li>
     * </ul>
     */
    private Job buildSkipFailJob(String jobName, JobMetadataRegistry.JobMetadata metadata,
                                  List<JobExecutionListener> listeners) throws JobExecutionException {
        
        if (metadata == null) {
            throw new JobExecutionException("SKIP_FAIL mode requires @BatchJob annotation");
        }

        List<String> stepNames = metadata.steps();
        log.info("SKIP_FAIL mode: building fault-tolerant flow with steps: {}", stepNames);

        // 构建容错 Flow
        Flow flow = buildFaultTolerantFlow(jobName, stepNames);

        JobBuilder jobBuilder = new JobBuilder(jobName + "_skipfail", jobRepository);
        for (JobExecutionListener listener : listeners) {
            jobBuilder.listener(listener);
        }
        
        // 添加 SKIP_FAIL 汇总监听器
        jobBuilder.listener(new SkipFailSummaryListener());

        return jobBuilder
            .start(flow)
            .end()
            .build();
    }

    /**
     * 构建 ISOLATED 模式 Job
     * 只执行指定的 Step
     * 
     * <p>当携带 ID 时，会尝试加载历史执行的 ExecutionContext</p>
     */
    private Job buildIsolatedJob(String jobName, JobMetadataRegistry.JobMetadata metadata,
                                  Long historicalId, String targetSteps, 
                                  List<JobExecutionListener> listeners) 
            throws JobExecutionException {
        
        List<String> stepsToExecute = Arrays.stream(targetSteps.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();

        log.info("ISOLATED mode: executing steps: {}, historicalId: {}", stepsToExecute, historicalId);

        // 构建只包含指定 Step 的容错 Flow（中间 Step 失败不阻断后续 Step）
        Flow flow = buildIsolatedFlow(jobName, stepsToExecute);

        JobBuilder jobBuilder = new JobBuilder(jobName + "_isolated", jobRepository);
        for (JobExecutionListener listener : listeners) {
            jobBuilder.listener(listener);
        }
        
        // 添加 ISOLATED 模式汇总监听器
        jobBuilder.listener(new IsolatedSummaryListener());
        
        // 如果携带历史 ID，添加上下文加载监听器
        if (historicalId != null) {
            JobExecution historicalExecution = executionStatusService.getJobExecution(historicalId);
            if (historicalExecution != null) {
                jobBuilder.listener(new ExecutionContextLoaderListener(historicalExecution));
                log.info("ISOLATED mode: will load ExecutionContext from historical execution id={}", historicalId);
            }
        }

        return jobBuilder
            .start(flow)
            .end()
            .build();
    }

    /**
     * 构建线性 Flow（用于 ISOLATED 模式的基础实现）
     */
    private Flow buildLinearFlow(String jobName, List<String> stepNames) throws JobExecutionException {
        if (stepNames == null || stepNames.isEmpty()) {
            throw new JobExecutionException("stepNames cannot be null or empty");
        }

        FlowBuilder<SimpleFlow> flowBuilder = new FlowBuilder<>(jobName + "_linearFlow");

        Step firstStep = getStepBean(stepNames.get(0));
        flowBuilder.start(firstStep);

        for (int i = 1; i < stepNames.size(); i++) {
            Step step = getStepBean(stepNames.get(i));
            flowBuilder.next(step);
        }

        return flowBuilder.build();
    }

    /**
     * 构建 ISOLATED 模式专用 Flow
     * 关键点：中间 Step 无论成功/失败都不中断后续 Step
     */
    private Flow buildIsolatedFlow(String jobName, List<String> stepNames) throws JobExecutionException {
        if (stepNames == null || stepNames.isEmpty()) {
            throw new JobExecutionException("stepNames cannot be null or empty");
        }

        FlowBuilder<SimpleFlow> flowBuilder = new FlowBuilder<>(jobName + "_isolatedFlow");
        List<Step> steps = new ArrayList<>();

        for (String stepName : stepNames) {
            steps.add(getStepBean(stepName));
        }

        if (steps.size() == 1) {
            // 单个 Step：直接执行，由 Spring Batch 决定最终 Job 状态
            flowBuilder.start(steps.get(0));
            return flowBuilder.build();
        }

        // 多个 Step：构建不中断链
        // Step1 --[任意状态]--> Step2 --[任意状态]--> Step3 ...
        flowBuilder.start(steps.get(0));

        for (int i = 0; i < steps.size() - 1; i++) {
            Step currentStep = steps.get(i);
            Step nextStep = steps.get(i + 1);

            // 无论当前 Step 成功还是失败，都转到下一个 Step
            flowBuilder.from(currentStep).on("*").to(nextStep);
        }

        return flowBuilder.build();
    }

    /**
     * 构建容错 Flow（用于 SKIP_FAIL 模式）
     * 
     * <p>关键点：使用 on("*") 匹配任意退出状态，包括 FAILED</p>
     * <p>Spring Batch Flow 的 "*" 通配符会匹配所有 ExitStatus</p>
     */
    private Flow buildFaultTolerantFlow(String jobName, List<String> stepNames) throws JobExecutionException {
        if (stepNames == null || stepNames.isEmpty()) {
            throw new JobExecutionException("stepNames cannot be null or empty");
        }

        FlowBuilder<SimpleFlow> flowBuilder = new FlowBuilder<>(jobName + "_faultTolerantFlow");
        List<Step> steps = new ArrayList<>();
        
        for (String stepName : stepNames) {
            steps.add(getStepBean(stepName));
        }

        if (steps.size() == 1) {
            // 单个 Step：直接执行，失败也标记为完成
            flowBuilder.start(steps.get(0))
                .on("FAILED").end("COMPLETED")
                .from(steps.get(0)).on("*").end();
            return flowBuilder.build();
        }

        // 多个 Step：构建容错链
        // Step1 --[任意状态]--> Step2 --[任意状态]--> Step3 ...
        flowBuilder.start(steps.get(0));
        
        for (int i = 0; i < steps.size() - 1; i++) {
            Step currentStep = steps.get(i);
            Step nextStep = steps.get(i + 1);
            
            // 无论当前 Step 成功还是失败，都转到下一个 Step
            flowBuilder.from(currentStep).on("*").to(nextStep);
        }
        
        // 最后一个 Step 的处理：失败也标记 Job 为完成
        Step lastStep = steps.get(steps.size() - 1);
        flowBuilder.from(lastStep).on("FAILED").end("COMPLETED");
        flowBuilder.from(lastStep).on("*").end();

        return flowBuilder.build();
    }

    /**
     * 获取 Step Bean
     */
    private Step getStepBean(String stepName) throws JobExecutionException {
        try {
            return applicationContext.getBean(stepName, Step.class);
        } catch (Exception e) {
            throw new JobExecutionException("Failed to get step bean: " + stepName, e);
        }
    }

    /**
     * SKIP_FAIL 模式汇总监听器
     * 在 Job 执行完成后汇总被跳过的 Step
     */
    @Slf4j
    private static class SkipFailSummaryListener implements JobExecutionListener {
        
        @Override
        public void afterJob(JobExecution jobExecution) {
            List<String> failedSteps = new ArrayList<>();
            List<String> completedSteps = new ArrayList<>();
            
            for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                if (stepExecution.getStatus() == BatchStatus.FAILED) {
                    failedSteps.add(stepExecution.getStepName());
                } else if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
                    completedSteps.add(stepExecution.getStepName());
                }
            }
            
            if (!failedSteps.isEmpty()) {
                log.warn("SKIP_FAIL mode summary: {} step(s) failed but were skipped: {}", 
                    failedSteps.size(), failedSteps);
                log.info("SKIP_FAIL mode summary: {} step(s) completed successfully: {}", 
                    completedSteps.size(), completedSteps);
                
                // 更新 Job 的 ExitStatus 添加警告信息
                String exitDescription = String.format(
                    "COMPLETED with %d skipped failures: %s", 
                    failedSteps.size(), failedSteps
                );
                jobExecution.setExitStatus(new ExitStatus("COMPLETED", exitDescription));
            }
        }
    }

    /**
     * ISOLATED 模式汇总监听器
     * 在 Job 执行完成后汇总失败的 Step（仅日志，不修改 Job 状态）
     */
    @Slf4j
    private static class IsolatedSummaryListener implements JobExecutionListener {

        @Override
        public void afterJob(JobExecution jobExecution) {
            List<String> failedSteps = new ArrayList<>();
            List<String> completedSteps = new ArrayList<>();

            for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
                if (stepExecution.getStatus() == BatchStatus.FAILED) {
                    failedSteps.add(stepExecution.getStepName());
                } else if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
                    completedSteps.add(stepExecution.getStepName());
                }
            }

            if (!failedSteps.isEmpty()) {
                log.warn("ISOLATED mode summary: {} step(s) failed: {}", failedSteps.size(), failedSteps);
                log.info("ISOLATED mode summary: {} step(s) completed successfully: {}", completedSteps.size(), completedSteps);
            }
        }
    }

    /**
     * ExecutionContext 加载监听器
     * 用于 ISOLATED + ID 模式，将历史执行的上下文加载到新执行中
     */
    @Slf4j
    @RequiredArgsConstructor
    private static class ExecutionContextLoaderListener implements JobExecutionListener {
        
        private final JobExecution historicalExecution;
        
        @Override
        public void beforeJob(JobExecution jobExecution) {
            if (historicalExecution == null) {
                return;
            }
            
            // 复制 Job 级别的 ExecutionContext
            ExecutionContext historicalJobContext = historicalExecution.getExecutionContext();
            if (historicalJobContext != null && !historicalJobContext.isEmpty()) {
                ExecutionContext currentContext = jobExecution.getExecutionContext();
                for (Map.Entry<String, Object> entry : historicalJobContext.entrySet()) {
                    currentContext.put(entry.getKey(), entry.getValue());
                }
                log.info("ISOLATED mode: loaded {} entries from historical Job ExecutionContext", 
                    historicalJobContext.size());
            }
            
            // 记录可用的历史 Step 上下文（供 Step 级别使用）
            for (StepExecution stepExecution : historicalExecution.getStepExecutions()) {
                ExecutionContext stepContext = stepExecution.getExecutionContext();
                if (stepContext != null && !stepContext.isEmpty()) {
                    // 将 Step 上下文以前缀形式存储到 Job 上下文
                    String prefix = "_historical_step_" + stepExecution.getStepName() + "_";
                    for (Map.Entry<String, Object> entry : stepContext.entrySet()) {
                        jobExecution.getExecutionContext().put(prefix + entry.getKey(), entry.getValue());
                    }
                    log.debug("ISOLATED mode: loaded {} entries from historical Step '{}' ExecutionContext",
                        stepContext.size(), stepExecution.getStepName());
                }
            }
        }
    }

    /**
     * 执行策略记录
     */
    public record ExecutionStrategy(
        ExecutionMode mode,
        List<String> stepsToExecute,
        boolean isResuming,
        String description
    ) {}
}
