package com.batchweaver.fileprocess.listener;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.time.Duration;

/**
 * 监控指标监听器
 * <p>
 * 集成Micrometer，记录Job/Step级指标
 */
@Slf4j
public class MetricsListener implements JobExecutionListener, StepExecutionListener {

    private final MeterRegistry meterRegistry;
    private Timer.Sample jobSample;
    private Timer.Sample stepSample;

    public MetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        jobSample = Timer.start(meterRegistry);
        log.info("Job started: {}", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        String status = jobExecution.getStatus().name();

        // Job执行时长
        jobSample.stop(Timer.builder("batch.job.duration")
            .tag("job_name", jobName)
            .tag("status", status)
            .register(meterRegistry));

        // Job读取/写入/跳过数量
        long readCount = jobExecution.getStepExecutions().stream()
            .mapToLong(StepExecution::getReadCount)
            .sum();
        long writeCount = jobExecution.getStepExecutions().stream()
            .mapToLong(StepExecution::getWriteCount)
            .sum();
        long skipCount = jobExecution.getStepExecutions().stream()
            .mapToLong(se -> se.getReadSkipCount() + se.getWriteSkipCount() + se.getProcessSkipCount())
            .sum();

        meterRegistry.counter("batch.job.read.count",
            "job_name", jobName).increment(readCount);
        meterRegistry.counter("batch.job.write.count",
            "job_name", jobName).increment(writeCount);
        meterRegistry.counter("batch.job.skip.count",
            "job_name", jobName).increment(skipCount);

        log.info("Job completed: {} - status={}, read={}, write={}, skip={}",
            jobName, status, readCount, writeCount, skipCount);
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepSample = Timer.start(meterRegistry);
        log.info("Step started: {}", stepExecution.getStepName());
    }

    @Override
    public void afterStep(StepExecution stepExecution) {
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        String stepName = stepExecution.getStepName();
        String status = stepExecution.getStatus().name();

        // Step执行时长
        stepSample.stop(Timer.builder("batch.step.duration")
            .tag("job_name", jobName)
            .tag("step_name", stepName)
            .tag("status", status)
            .register(meterRegistry));

        // Step读取/写入/跳过数量
        meterRegistry.counter("batch.step.read.count",
            "job_name", jobName, "step_name", stepName).increment(stepExecution.getReadCount());
        meterRegistry.counter("batch.step.write.count",
            "job_name", jobName, "step_name", stepName).increment(stepExecution.getWriteCount());
        meterRegistry.counter("batch.step.skip.count",
            "job_name", jobName, "step_name", stepName).increment(
            stepExecution.getReadSkipCount() + stepExecution.getWriteSkipCount() + stepExecution.getProcessSkipCount());

        // 吞吐量（records/s）
        Duration duration = Duration.between(stepExecution.getStartTime(), stepExecution.getEndTime());
        double throughput = stepExecution.getWriteCount() / (duration.toMillis() / 1000.0);
        meterRegistry.gauge("batch.step.throughput",
            java.util.List.of(
                io.micrometer.core.instrument.Tag.of("job_name", jobName),
                io.micrometer.core.instrument.Tag.of("step_name", stepName)
            ), throughput);

        log.info("Step completed: {} - status={}, read={}, write={}, skip={}, throughput={} records/s",
            stepName, status, stepExecution.getReadCount(), stepExecution.getWriteCount(),
            stepExecution.getReadSkipCount() + stepExecution.getWriteSkipCount() + stepExecution.getProcessSkipCount(),
            String.format("%.2f", throughput));

        return null;
    }
}
