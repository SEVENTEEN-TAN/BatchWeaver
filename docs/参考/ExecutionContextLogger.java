package com.example.batch.core.logging;

import com.example.batch.core.execution.ExecutionMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 执行上下文日志记录器
 * 提供启动契约（Execution Contract）和执行摘要（Execution Summary）
 */
@Slf4j
@Component
public class ExecutionContextLogger implements JobExecutionListener, StepExecutionListener {

    private static final String LINE = "=".repeat(80);
    private static final String SEPARATOR = "-".repeat(78);

    // 使用线程安全集合支持并行 Step
    private final List<StepSummary> stepSummaries = new CopyOnWriteArrayList<>();

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // 清空上次执行的 Step 汇总，防止跨 Job 污染
        stepSummaries.clear();

        ExecutionMode mode = ExecutionMode.from(jobExecution.getJobParameters().getString("_mode"));

        StringBuilder contract = new StringBuilder("\n");
        contract.append(LINE).append("\n");
        contract.append("  BATCH WEAVER EXECUTION CONTRACT\n");
        contract.append(LINE).append("\n");
        contract.append(String.format("  Job Name       : %s%n", jobExecution.getJobInstance().getJobName()));
        contract.append(String.format("  Execution ID   : %d%n", jobExecution.getId()));
        contract.append(String.format("  Mode           : [ %s ]", mode));

        if (mode.requiresWarning()) {
            contract.append(" ⚠️ SPECIAL MODE ENABLED");
        }
        contract.append("\n");

        contract.append("\n  Strategy       :\n");
        contract.append(String.format("    - %s%n", mode.getDescription()));

        // 特殊模式的额外提示
        if (mode == ExecutionMode.SKIP_FAIL) {
            contract.append("    - Failed steps will be marked as SKIPPED in metrics\n");
        } else if (mode == ExecutionMode.ISOLATED) {
            String targetSteps = jobExecution.getJobParameters().getString("_target_steps");
            contract.append(String.format("    - Target steps: %s%n", targetSteps));
        } else if (mode == ExecutionMode.RESUME) {
            Long id = jobExecution.getJobParameters().getLong("id");
            Long resumeId = jobExecution.getJobParameters().getLong("_resume_id");
            Long effectiveId = resumeId != null ? resumeId : id;
            if (effectiveId != null) {
                contract.append(String.format("    - Resuming from execution ID: %d%n", effectiveId));
            }
        }

        contract.append(LINE);

        log.info(contract.toString());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Duration duration = Duration.between(
                jobExecution.getStartTime().toInstant(ZoneOffset.UTC),
                jobExecution.getEndTime() != null
                        ? jobExecution.getEndTime().toInstant(ZoneOffset.UTC)
                        : java.time.Instant.now()
        );

        // 直接从 JobExecution 获取 Step 执行信息
        List<StepSummary> steps = new ArrayList<>();
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            long stepDurationMs = 0;
            if (stepExecution.getStartTime() != null && stepExecution.getEndTime() != null) {
                stepDurationMs = Duration.between(
                        stepExecution.getStartTime().toInstant(ZoneOffset.UTC),
                        stepExecution.getEndTime().toInstant(ZoneOffset.UTC)
                ).toMillis();
            }
            steps.add(new StepSummary(
                    stepExecution.getStepName(),
                    stepExecution.getExitStatus().getExitCode(),
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getSkipCount(),
                    stepDurationMs
            ));
        }

        boolean hasWarnings = steps.stream().anyMatch(s ->
                s.exitStatus().equals("ABANDONED") || s.exitStatus().equals("FAILED") || s.skipCount() > 0
        );

        StringBuilder summary = new StringBuilder("\n");
        summary.append(LINE).append("\n");
        summary.append("  EXECUTION SUMMARY\n");
        summary.append(LINE).append("\n");
        summary.append(String.format("  Total Duration : %d min %d sec%n", duration.toMinutes(), duration.toSecondsPart()));
        summary.append(String.format("  Final Status   : %s", jobExecution.getExitStatus().getExitCode()));

        if (hasWarnings) {
            summary.append(" (with warnings)");
        }
        summary.append("\n\n");

        summary.append("  Step Details   :\n");
        summary.append("  " + SEPARATOR + "\n");
        summary.append(String.format("  | %-20s | %-12s | %6s | %6s | %4s | %8s | %-15s |%n",
                "Step Name", "Status", "Read", "Write", "Skip", "Duration", "Note"));
        summary.append("  " + SEPARATOR + "\n");

        for (StepSummary step : steps) {
            String note = "";
            if (step.exitStatus().equals("ABANDONED") || step.skipCount() > 0) {
                note = "[IGNORED FAILURE]";
            } else if (step.exitStatus().equals("FAILED")) {
                note = "[FAILED]";
            }

            summary.append(String.format("  | %-20s | %-12s | %6d | %6d | %4d | %8s | %-15s |%n",
                    step.stepName(),
                    step.exitStatus(),
                    step.readCount(),
                    step.writeCount(),
                    step.skipCount(),
                    formatDuration(step.durationMs()),
                    note
            ));
        }

        summary.append("  " + SEPARATOR + "\n");

        // 添加行动建议
        if (hasWarnings) {
            summary.append("\n  Action Required:\n");
            ExecutionMode mode = ExecutionMode.from(jobExecution.getJobParameters().getString("_mode"));
            if (mode == ExecutionMode.SKIP_FAIL) {
                summary.append("    - Some steps failed but were skipped per mode=SKIP_FAIL.\n");
                summary.append("    - Please check logs for exception details.\n");
            }
            if (jobExecution.getExitStatus().getExitCode().equals(ExitStatus.FAILED.getExitCode())) {
                summary.append(String.format("    - Job FAILED. Retry with '_mode=RESUME id=%d' to continue from breakpoint.%n",
                        jobExecution.getId()));
            }
        }

        summary.append(LINE);

        log.info(summary.toString());
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Starting Step: {}", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long durationMs = stepExecution.getEndTime() != null && stepExecution.getStartTime() != null
                ? Duration.between(
                stepExecution.getStartTime().toInstant(ZoneOffset.UTC),
                stepExecution.getEndTime().toInstant(ZoneOffset.UTC)
        ).toMillis()
                : 0;

        StepSummary summary = new StepSummary(
                stepExecution.getStepName(),
                stepExecution.getExitStatus().getExitCode(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                durationMs
        );

        stepSummaries.add(summary);

        log.info("Step '{}' completed: status={}, read={}, write={}, skip={}, duration={}ms",
                stepExecution.getStepName(),
                stepExecution.getExitStatus().getExitCode(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                durationMs
        );

        // 零数据警告（仅在 ISOLATED 模式）
        ExecutionMode mode = ExecutionMode.from(
                stepExecution.getJobExecution().getJobParameters().getString("_mode")
        );
        if (mode == ExecutionMode.ISOLATED && stepExecution.getReadCount() == 0) {
            log.warn("[ISOLATED MODE] Step '{}' executed successfully but processed 0 records. Ensure upstream data exists.",
                    stepExecution.getStepName());
        }

        return stepExecution.getExitStatus();
    }

    private String formatDuration(long durationMs) {
        // 支持毫秒级精度
        if (durationMs < 1000) {
            return durationMs + "ms";
        }

        long seconds = durationMs / 1000;
        if (seconds < 60) {
            return seconds + "s";
        } else {
            return (seconds / 60) + "m" + (seconds % 60) + "s";
        }
    }

    /**
     * Step 执行摘要记录
     */
    private record StepSummary(
            String stepName,
            String exitStatus,
            long readCount,
            long writeCount,
            long skipCount,
            long durationMs
    ) {
    }
}
