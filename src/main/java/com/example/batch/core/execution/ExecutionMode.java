package com.example.batch.core.execution;

import lombok.extern.slf4j.Slf4j;

/**
 * Job 执行模式枚举
 * 通过 JobParameter "_mode" 控制 Job 的执行策略
 */
@Slf4j
public enum ExecutionMode {

    /**
     * 标准模式：按定义的顺序执行所有 Step，支持条件流转
     */
    STANDARD,

    /**
     * 断点续传模式：从上次失败的 Step 继续执行
     * 要求：JobParameters 中的 id 必须与历史 JobInstance 一致
     */
    RESUME,

    /**
     * 跳过失败模式：Step 失败时标记为 SKIPPED 并继续执行后续 Step
     */
    SKIP_FAIL,

    /**
     * 独立 Step 模式：仅执行指定的 Step（通过 _target_steps 参数）
     */
    ISOLATED;

    /**
     * 从字符串解析执行模式（忽略大小写）
     * 如果模式不合法，返回 STANDARD 并记录警告
     *
     * @param mode 模式字符串
     * @return ExecutionMode 枚举值
     */
    public static ExecutionMode from(String mode) {
        if (mode == null || mode.trim().isEmpty()) {
            return STANDARD;
        }

        try {
            return ExecutionMode.valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid execution mode: '{}'. Falling back to STANDARD. Valid modes: STANDARD, RESUME, SKIP_FAIL, ISOLATED", mode);
            return STANDARD;
        }
    }

    /**
     * 获取模式描述（用于日志输出）
     */
    public String getDescription() {
        return switch (this) {
            case STANDARD -> "Standard sequential execution with conditional flows";
            case RESUME -> "Resume from last failed step (requires matching JobInstance)";
            case SKIP_FAIL -> "Skip failed steps and continue execution";
            case ISOLATED -> "Execute specific steps only (requires _target_steps parameter)";
        };
    }

    /**
     * 判断是否需要风险提示
     */
    public boolean requiresWarning() {
        return this == SKIP_FAIL || this == ISOLATED;
    }
}
