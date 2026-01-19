package com.batchweaver.core.fileprocess.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 执行上下文日志记录器
 * <p>
 * 提供格式化的执行报告：启动契约 + 执行摘要
 */
@Slf4j
@Component
public class ExecutionContextLogger implements JobExecutionListener, StepExecutionListener {

    private static final String LINE = "=".repeat(80);
    private static final String SEPARATOR = "-".repeat(78);

    @Override
    public void beforeJob(JobExecution jobExecution) {

        StringBuilder contract = new StringBuilder("\n");
        contract.append(LINE).append("\n");
        contract.append("  BATCH EXECUTION CONTRACT\n");
        contract.append(LINE).append("\n");
        contract.append(String.format("  Job Name       : %s%n", jobExecution.getJobInstance().getJobName()));
        contract.append(String.format("  Execution ID   : %d%n", jobExecution.getId()));
        contract.append(String.format("  Create Time    : %s%n", jobExecution.getCreateTime()));
        contract.append(LINE);

        log.info(contract.toString());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();

        Duration duration = Duration.ZERO;
        if (startTime != null && endTime != null) {
            duration = Duration.between(startTime, endTime);
        }

        List<StepSummary> steps = new ArrayList<>();
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            long stepDurationMs = 0;
            if (stepExecution.getStartTime() != null && stepExecution.getEndTime() != null) {
                stepDurationMs = Duration.between(
                    stepExecution.getStartTime(),
                    stepExecution.getEndTime()
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
            s.exitStatus().equals("FAILED") || s.skipCount() > 0
        );

        StringBuilder summary = new StringBuilder("\n");
        summary.append(LINE).append("\n");
        summary.append("  EXECUTION SUMMARY\n");
        summary.append(LINE).append("\n");
        summary.append(String.format("  Total Duration : %d min %d sec%n",
            duration.toMinutes(), duration.toSecondsPart()));
        summary.append(String.format("  Final Status   : %s",
            jobExecution.getExitStatus().getExitCode()));

        if (hasWarnings) {
            summary.append(" (with warnings)");
        }
        summary.append("\n\n");

        summary.append("  Step Details   :\n");
        summary.append("  " + SEPARATOR + "\n");
        summary.append(String.format("  | %-20s | %-12s | %6s | %6s | %4s | %8s |%n",
            "Step Name", "Status", "Read", "Write", "Skip", "Duration"));
        summary.append("  " + SEPARATOR + "\n");

        for (StepSummary step : steps) {
            summary.append(String.format("  | %-20s | %-12s | %6d | %6d | %4d | %8s |%n",
                step.stepName(),
                step.exitStatus(),
                step.readCount(),
                step.writeCount(),
                step.skipCount(),
                formatDuration(step.durationMs())
            ));
        }

        summary.append("  " + SEPARATOR + "\n");
        summary.append(LINE);

        log.info(summary.toString());
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("Starting Step: {}", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long durationMs = 0;
        if (stepExecution.getEndTime() != null && stepExecution.getStartTime() != null) {
            durationMs = Duration.between(
                stepExecution.getStartTime(),
                stepExecution.getEndTime()
            ).toMillis();
        }

        log.info("Step '{}' completed: status={}, read={}, write={}, skip={}, duration={}",
            stepExecution.getStepName(),
            stepExecution.getExitStatus().getExitCode(),
            stepExecution.getReadCount(),
            stepExecution.getWriteCount(),
            stepExecution.getSkipCount(),
            formatDuration(durationMs)
        );

        return stepExecution.getExitStatus();
    }

    private String formatDuration(long durationMs) {
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

    private record StepSummary(
        String stepName,
        String exitStatus,
        long readCount,
        long writeCount,
        long skipCount,
        long durationMs
    ) {}
}
