package com.example.batch.core.validation;

import com.example.batch.core.execution.ExecutionMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Job 参数格式校验器
 * 在执行模式语义校验之前，对参数格式进行前置校验
 *
 * 校验内容：
 * - 必需参数是否存在
 * - 参数格式是否正确
 * - 参数组合是否合法
 */
@Slf4j
@Component
public class JobParametersFormatValidator {

    /**
     * 校验参数格式
     *
     * @param jobName     Job 名称
     * @param mode        执行模式
     * @param idStr       id 参数字符串（可为 null）
     * @param targetSteps 目标 Step 列表（可为 null）
     * @return ValidationResult 校验结果
     */
    public ValidationResult validate(String jobName, ExecutionMode mode, String idStr, String targetSteps) {
        List<String> errors = new ArrayList<>();

        // 1. 校验 jobName
        if (jobName == null || jobName.trim().isEmpty()) {
            errors.add("Parameter 'jobName' is required.");
            return ValidationResult.fail(errors);
        }

        // 2. 根据执行模式校验参数
        switch (mode) {
            case STANDARD -> validateStandardParams(idStr, errors);
            case RESUME -> validateResumeParams(idStr, errors);
            case SKIP_FAIL -> validateSkipFailParams(idStr, errors);
            case ISOLATED -> validateIsolatedParams(idStr, targetSteps, errors);
        }

        if (!errors.isEmpty()) {
            return ValidationResult.fail(errors);
        }

        log.debug("Parameter format validation passed: jobName={}, mode={}, id={}, targetSteps={}",
            jobName, mode, idStr, targetSteps);
        return ValidationResult.success();
    }

    /**
     * 校验 STANDARD 模式参数格式
     * 规则：不能携带 id 参数
     */
    private void validateStandardParams(String idStr, List<String> errors) {
        if (idStr != null && !idStr.trim().isEmpty()) {
            errors.add("STANDARD mode does not accept 'id' parameter.");
            errors.add("  → To resume a failed execution, use: _mode=RESUME id=<executionId>");
            errors.add("  → To re-run specific steps, use: _mode=ISOLATED _target_steps=<stepNames>");
        }
    }

    /**
     * 校验 RESUME 模式参数格式
     * 规则：必须携带 id 参数，且 id 必须是正整数
     */
    private void validateResumeParams(String idStr, List<String> errors) {
        if (idStr == null || idStr.trim().isEmpty()) {
            errors.add("RESUME mode requires 'id' parameter (JobExecution ID).");
            errors.add("  → Query failed execution ID from metadata table:");
            errors.add("     SELECT JOB_EXECUTION_ID, STATUS, START_TIME");
            errors.add("     FROM BATCH_JOB_EXECUTION");
            errors.add("     WHERE JOB_NAME = '<yourJobName>' AND STATUS = 'FAILED'");
            errors.add("     ORDER BY START_TIME DESC");
            errors.add("  → Then run: jobName=<jobName> _mode=RESUME id=<executionId>");
            return;
        }

        // 校验 id 格式
        if (!isPositiveInteger(idStr)) {
            errors.add(String.format("Parameter 'id' must be a positive integer, got: '%s'", idStr));
            errors.add("  → Example: jobName=myJob _mode=RESUME id=1001");
        }
    }

    /**
     * 校验 SKIP_FAIL 模式参数格式
     * 规则：不能携带 id 参数
     */
    private void validateSkipFailParams(String idStr, List<String> errors) {
        if (idStr != null && !idStr.trim().isEmpty()) {
            errors.add("SKIP_FAIL mode does not accept 'id' parameter.");
            errors.add("  → SKIP_FAIL creates a new execution that skips failed steps.");
            errors.add("  → To resume a historical execution, use: _mode=RESUME id=<executionId>");
            errors.add("  → To re-run specific steps from history, use: _mode=ISOLATED id=<executionId> _target_steps=<stepNames>");
        }
    }

    /**
     * 校验 ISOLATED 模式参数格式
     * 规则：必须携带 _target_steps 参数，id 可选但格式必须正确
     */
    private void validateIsolatedParams(String idStr, String targetSteps, List<String> errors) {
        // 1. 校验 _target_steps 参数
        if (targetSteps == null || targetSteps.trim().isEmpty()) {
            errors.add("ISOLATED mode requires '_target_steps' parameter.");
            errors.add("  → Specify step names to execute (comma-separated):");
            errors.add("     jobName=myJob _mode=ISOLATED _target_steps=step1,step2");
            errors.add("  → To run with historical context:");
            errors.add("     jobName=myJob _mode=ISOLATED id=<executionId> _target_steps=step3");
            return;
        }

        // 2. 校验 _target_steps 格式（不能全是空白/逗号）
        String[] steps = targetSteps.split(",");
        boolean hasValidStep = false;
        for (String step : steps) {
            if (!step.trim().isEmpty()) {
                hasValidStep = true;
                break;
            }
        }
        if (!hasValidStep) {
            errors.add("Parameter '_target_steps' cannot be empty or contain only commas.");
            errors.add("  → Example: _target_steps=step1,step2,step3");
        }

        // 3. 如果提供了 id，校验其格式
        if (idStr != null && !idStr.trim().isEmpty() && !isPositiveInteger(idStr)) {
            errors.add(String.format("Parameter 'id' must be a positive integer, got: '%s'", idStr));
            errors.add("  → Example: jobName=myJob _mode=ISOLATED id=1001 _target_steps=step1");
        }
    }

    /**
     * 校验字符串是否为正整数
     */
    private boolean isPositiveInteger(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        String trimmed = str.trim();
        if (trimmed.length() == 0) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        try {
            long val = Long.parseLong(trimmed);
            return val > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 校验结果
     */
    public static class ValidationResult {
        private final boolean success;
        private final List<String> errors;

        private ValidationResult(boolean success, List<String> errors) {
            this.success = success;
            this.errors = errors;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult fail(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        public boolean isSuccess() {
            return success;
        }

        public List<String> getErrors() {
            return errors;
        }

        /**
         * 获取格式化的错误消息
         */
        public String getFormattedErrorMessage() {
            if (success) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Parameter validation failed:\n");
            for (String error : errors) {
                sb.append("  ").append(error).append("\n");
            }
            return sb.toString().trim();
        }
    }
}
