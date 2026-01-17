package com.example.batch.core.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

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

    @PostConstruct
    public void warnAboutStepsOrder() {
        log.warn("========================================");
        log.warn("@BatchJob.steps 顺序依赖警告");
        log.warn("RESUME 和 SKIP_FAIL 模式依赖 steps 数组的顺序决定续传/容错执行路径");
        log.warn("请确保 @BatchJob.steps 顺序与 Job 实际执行顺序严格一致");
        log.warn("========================================");
    }

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
            case ISOLATED -> validateIsolatedMode(targetSteps, metadata, jobName, id);
        }

        // 语义层面的统一出口日志，方便追踪一次执行的模式与关键参数
        log.info("Execution mode validated: jobName={}, mode={}, id={}, targetSteps={}",
            jobName, mode, id, targetSteps);
    }

    /**
     * 校验 STANDARD 模式
     * 规则：不能带 ID
     */
    private void validateStandardMode(Long id) throws ExecutionModeValidationException {
        if (id != null) {
            throw new ExecutionModeValidationException(String.format(
                "STANDARD mode does not accept 'id' parameter (found id=%d).\n" +
                "  → STANDARD mode always creates a new execution.\n" +
                "  → To resume a failed execution, use: _mode=RESUME id=%d\n" +
                "  → To re-run specific steps, use: _mode=ISOLATED id=%d _target_steps=<stepNames>",
                id, id, id
            ));
        }
    }

    /**
     * 校验 RESUME 模式
     * 规则：必须带 ID，历史模式不能是 SKIP_FAIL/ISOLATED
     */
    private void validateResumeMode(Long id, String jobName) throws ExecutionModeValidationException {
        if (id == null) {
            throw new ExecutionModeValidationException(
                "RESUME mode requires 'id' parameter (JobExecution ID).\n" +
                "  → Query failed execution from metadata table:\n" +
                "     SELECT JOB_EXECUTION_ID, STATUS, START_TIME FROM BATCH_JOB_EXECUTION\n" +
                "     WHERE JOB_NAME = '" + jobName + "' AND STATUS = 'FAILED'\n" +
                "     ORDER BY START_TIME DESC"
            );
        }

        // 查询历史执行
        JobExecution historicalExecution = executionStatusService.getJobExecution(id);
        if (historicalExecution == null) {
            throw new ExecutionModeValidationException(String.format(
                "No execution found for id=%d.\n" +
                "  → Verify the execution ID exists in BATCH_JOB_EXECUTION table.\n" +
                "  → Query: SELECT * FROM BATCH_JOB_EXECUTION WHERE JOB_EXECUTION_ID = %d",
                id, id
            ));
        }

        // CHECK: Job 名称必须匹配，防止跨 Job 误续传
        String historicalJobName = historicalExecution.getJobInstance().getJobName();
        if (!historicalJobName.equals(jobName)) {
            throw new ExecutionModeValidationException(String.format(
                "Job name mismatch for id=%d.\n" +
                "  → Historical jobName: '%s'\n" +
                "  → Requested jobName: '%s'\n" +
                "  → RESUME requires matching job names to prevent cross-job resume.",
                id, historicalJobName, jobName
            ));
        }

        // 检查历史执行是否已完成（保持原有友好提示）
        if (executionStatusService.isCompletedSuccessfully(historicalExecution)) {
            throw new ExecutionModeValidationException(String.format(
                "Job already completed successfully (id=%d). No steps to resume.\n" +
                "  → Use _mode=ISOLATED with _target_steps to re-run specific steps.",
                id
            ));
        }

        // 仅允许从 FAILED 状态续传（STOPPED/ABANDONED 等其他状态视为非法）
        if (historicalExecution.getStatus() != BatchStatus.FAILED) {
            throw new ExecutionModeValidationException(String.format(
                "JobExecution[id=%d] status is %s, only FAILED status is allowed for RESUME.\n" +
                "  → RESUME mode can only continue from FAILED executions.\n" +
                "  → Current status does not support automatic resume.",
                id, historicalExecution.getStatus()
            ));
        }

        // 检查历史执行模式
        String historicalMode = historicalExecution.getJobParameters().getString("_mode", "STANDARD");
        if ("SKIP_FAIL".equals(historicalMode)) {
            throw new ExecutionModeValidationException(String.format(
                "Cannot RESUME after SKIP_FAIL execution (id=%d).\n" +
                "  → SKIP_FAIL mode breaks the execution flow by skipping failures.\n" +
                "  → Use _mode=ISOLATED _target_steps=<stepNames> to re-run specific steps.",
                id
            ));
        }
        if ("ISOLATED".equals(historicalMode)) {
            throw new ExecutionModeValidationException(String.format(
                "Cannot RESUME after ISOLATED execution (id=%d).\n" +
                "  → ISOLATED mode executes steps independently, breaking flow continuity.\n" +
                "  → Use _mode=ISOLATED id=%d _target_steps=<stepNames> to continue isolated execution.",
                id, id
            ));
        }

        // 检查是否有失败的 Step（理论上 FAILED 状态必然存在，但此处作为兜底防御）
        if (!executionStatusService.hasFailedSteps(historicalExecution)) {
            throw new ExecutionModeValidationException(String.format(
                "No failed steps found for id=%d. Nothing to resume.\n" +
                "  → This is unexpected for a FAILED execution. Check execution metadata.",
                id
            ));
        }
    }

    /**
     * 校验 SKIP_FAIL 模式
     * 规则：不能带 ID，不能用于条件流 Job
     */
    private void validateSkipFailMode(Long id, JobMetadataRegistry.JobMetadata metadata)
            throws ExecutionModeValidationException {

        if (id != null) {
            throw new ExecutionModeValidationException(String.format(
                "SKIP_FAIL mode does not accept 'id' parameter (found id=%d).\n" +
                "  → SKIP_FAIL always creates a new execution that tolerates step failures.\n" +
                "  → To resume a failed execution, use: _mode=RESUME id=%d\n" +
                "  → To re-run specific steps from history, use: _mode=ISOLATED id=%d _target_steps=<stepNames>",
                id, id, id
            ));
        }

        if (metadata != null && metadata.conditionalFlow()) {
            throw new ExecutionModeValidationException(String.format(
                "SKIP_FAIL mode is not supported for conditional flow job '%s'.\n" +
                "  → Conditional flows have complex branching logic that conflicts with skip-on-fail semantics.\n" +
                "  → Use _mode=STANDARD for normal execution.\n" +
                "  → Use _mode=ISOLATED _target_steps=<stepNames> to re-run specific steps.",
                metadata.name()
            ));
        }
    }

    /**
     * 校验 ISOLATED 模式
     * 规则：必须有 _target_steps，Step 名称必须合法；如携带 id，则必须存在且 jobName 匹配
     */
    private void validateIsolatedMode(String targetSteps,
                                       JobMetadataRegistry.JobMetadata metadata,
                                       String jobName,
                                       Long id) throws ExecutionModeValidationException {

        if (targetSteps == null || targetSteps.trim().isEmpty()) {
            throw new ExecutionModeValidationException(
                "ISOLATED mode requires '_target_steps' parameter.\n" +
                "  → Specify step names to execute (comma-separated):\n" +
                "     jobName=" + jobName + " _mode=ISOLATED _target_steps=step1,step2\n" +
                "  → To run with historical context:\n" +
                "     jobName=" + jobName + " _mode=ISOLATED id=<executionId> _target_steps=step3"
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
                    throw new ExecutionModeValidationException(String.format(
                        "Invalid step name '%s' for job '%s'.\n" +
                        "  → Valid steps: %s\n" +
                        "  → Check @BatchJob annotation or Job configuration.",
                        step, jobName, metadata.steps()
                    ));
                }
            }
        }

        // 如携带 id，则必须存在且 jobName 匹配
        if (id != null) {
            JobExecution historicalExecution = executionStatusService.getJobExecution(id);
            if (historicalExecution == null) {
                throw new ExecutionModeValidationException(String.format(
                    "No execution found for id=%d in ISOLATED mode.\n" +
                    "  → Verify the execution ID exists in BATCH_JOB_EXECUTION table.\n" +
                    "  → Query: SELECT * FROM BATCH_JOB_EXECUTION WHERE JOB_EXECUTION_ID = %d",
                    id, id
                ));
            }

            String historicalJobName = historicalExecution.getJobInstance().getJobName();
            if (!historicalJobName.equals(jobName)) {
                throw new ExecutionModeValidationException(String.format(
                    "Job name mismatch for id=%d in ISOLATED mode.\n" +
                    "  → Historical jobName: '%s'\n" +
                    "  → Requested jobName: '%s'\n" +
                    "  → ISOLATED requires matching job names to load correct execution context.",
                    id, historicalJobName, jobName
                ));
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
