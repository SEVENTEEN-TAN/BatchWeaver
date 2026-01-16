package com.example.batch.core.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 执行模式校验器
 * 校验执行模式、Job 类型、ID 规则等约束
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionModeValidator {

    private final JobMetadataRegistry jobMetadataRegistry;
    private final ExecutionStatusService executionStatusService;

    /**
     * 校验执行模式参数
     *
     * @param jobName      Job 名称
     * @param mode         执行模式
     * @param id           历史 Execution ID（可为 null）
     * @param targetSteps  目标 Step 列表（ISOLATED 模式使用）
     * @throws ExecutionModeValidationException 如果校验失败
     */
    public void validate(String jobName, ExecutionMode mode, Long id, String targetSteps) 
            throws ExecutionModeValidationException {
        
        JobMetadataRegistry.JobMetadata metadata = jobMetadataRegistry.get(jobName);
        
        // 未注册的 Job 只能使用 STANDARD 模式
        if (metadata == null && mode != ExecutionMode.STANDARD) {
            throw new ExecutionModeValidationException(
                String.format("Job '%s' does not support advanced execution modes. " +
                    "Add @BatchJob annotation to enable.", jobName)
            );
        }

        // 根据模式校验
        switch (mode) {
            case STANDARD -> validateStandardMode(id);
            case RESUME -> validateResumeMode(id, jobName);
            case SKIP_FAIL -> validateSkipFailMode(id, metadata);
            case ISOLATED -> validateIsolatedMode(targetSteps, metadata, jobName);
        }
    }

    /**
     * 校验 STANDARD 模式
     * 规则：不能带 ID
     */
    private void validateStandardMode(Long id) throws ExecutionModeValidationException {
        if (id != null) {
            throw new ExecutionModeValidationException(
                "STANDARD mode does not accept 'id' parameter. " +
                "Use RESUME or ISOLATED for historical executions."
            );
        }
    }

    /**
     * 校验 RESUME 模式
     * 规则：必须带 ID，历史模式不能是 SKIP_FAIL/ISOLATED
     */
    private void validateResumeMode(Long id, String jobName) throws ExecutionModeValidationException {
        if (id == null) {
            throw new ExecutionModeValidationException(
                "RESUME mode requires 'id' parameter. " +
                "Query failed execution ID from metadata table."
            );
        }

        // 查询历史执行
        JobExecution historicalExecution = executionStatusService.getJobExecution(id);
        if (historicalExecution == null) {
            throw new ExecutionModeValidationException(
                String.format("No execution found for id=%d.", id)
            );
        }

        // 检查历史执行是否已完成
        if (executionStatusService.isCompletedSuccessfully(historicalExecution)) {
            throw new ExecutionModeValidationException(
                String.format("Job already completed successfully (id=%d). No steps to resume.", id)
            );
        }

        // 检查历史执行模式
        String historicalMode = historicalExecution.getJobParameters().getString("_mode", "STANDARD");
        if ("SKIP_FAIL".equals(historicalMode)) {
            throw new ExecutionModeValidationException(
                String.format("Cannot RESUME after SKIP_FAIL execution (id=%d). " +
                    "Use ISOLATED mode instead.", id)
            );
        }
        if ("ISOLATED".equals(historicalMode)) {
            throw new ExecutionModeValidationException(
                String.format("Cannot RESUME after ISOLATED execution (id=%d). " +
                    "Use ISOLATED mode instead.", id)
            );
        }

        // 检查是否有失败的 Step
        if (!executionStatusService.hasFailedSteps(historicalExecution)) {
            throw new ExecutionModeValidationException(
                String.format("No failed steps found for id=%d. Nothing to resume.", id)
            );
        }
    }

    /**
     * 校验 SKIP_FAIL 模式
     * 规则：不能带 ID，不能用于条件流 Job
     */
    private void validateSkipFailMode(Long id, JobMetadataRegistry.JobMetadata metadata) 
            throws ExecutionModeValidationException {
        
        if (id != null) {
            throw new ExecutionModeValidationException(
                "SKIP_FAIL mode does not accept 'id' parameter. " +
                "Use RESUME or ISOLATED for historical executions."
            );
        }

        if (metadata != null && metadata.conditionalFlow()) {
            throw new ExecutionModeValidationException(
                "SKIP_FAIL mode is not supported for conditional flow jobs. " +
                "Use STANDARD or ISOLATED."
            );
        }
    }

    /**
     * 校验 ISOLATED 模式
     * 规则：必须有 _target_steps，Step 名称必须合法
     */
    private void validateIsolatedMode(String targetSteps, 
                                       JobMetadataRegistry.JobMetadata metadata, 
                                       String jobName) throws ExecutionModeValidationException {
        
        if (targetSteps == null || targetSteps.trim().isEmpty()) {
            throw new ExecutionModeValidationException(
                "ISOLATED mode requires '_target_steps' parameter."
            );
        }

        // 如果 Job 已注册，校验 Step 名称合法性
        if (metadata != null) {
            List<String> requestedSteps = Arrays.stream(targetSteps.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

            for (String step : requestedSteps) {
                if (!metadata.steps().contains(step)) {
                    throw new ExecutionModeValidationException(
                        String.format("Invalid step name '%s'. Valid steps for job '%s': %s",
                            step, jobName, metadata.steps())
                    );
                }
            }
        }
    }

    /**
     * 执行模式校验异常
     */
    public static class ExecutionModeValidationException extends Exception {
        public ExecutionModeValidationException(String message) {
            super(message);
        }
    }
}
