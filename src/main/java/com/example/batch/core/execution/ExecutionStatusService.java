package com.example.batch.core.execution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 执行状态服务
 * 封装历史 Job/Step 执行状态的查询逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionStatusService {

    private final JobExplorer jobExplorer;

    /**
     * 根据 Execution ID 获取历史 JobExecution
     *
     * @param executionId Job Execution ID
     * @return JobExecution 或 null（如果不存在）
     */
    public JobExecution getJobExecution(Long executionId) {
        if (executionId == null) {
            return null;
        }
        return jobExplorer.getJobExecution(executionId);
    }

    /**
     * 检查 Job 是否已完成且无异常
     *
     * @param jobExecution 历史执行记录
     * @return true 如果已完成且无异常
     */
    public boolean isCompletedSuccessfully(JobExecution jobExecution) {
        if (jobExecution == null) {
            return false;
        }
        return jobExecution.getStatus() == BatchStatus.COMPLETED;
    }

    /**
     * 检查 Job 是否有失败的 Step
     *
     * @param jobExecution 历史执行记录
     * @return true 如果有失败的 Step
     */
    public boolean hasFailedSteps(JobExecution jobExecution) {
        if (jobExecution == null) {
            return false;
        }
        return jobExecution.getStepExecutions().stream()
            .anyMatch(step -> step.getStatus() == BatchStatus.FAILED);
    }

    /**
     * 获取第一个失败的 Step 名称
     *
     * @param jobExecution 历史执行记录
     * @return 第一个失败的 Step 名称，或 null
     */
    public String getFirstFailedStepName(JobExecution jobExecution) {
        if (jobExecution == null) {
            return null;
        }

        // 按开始时间排序，找到第一个失败的 Step
        return jobExecution.getStepExecutions().stream()
            .filter(step -> step.getStatus() == BatchStatus.FAILED)
            .min(Comparator.comparing(StepExecution::getStartTime))
            .map(StepExecution::getStepName)
            .orElse(null);
    }

    /**
     * 获取从第一个失败 Step 开始需要重新执行的 Step 列表
     * 用于 RESUME 模式：从第一个失败的 Step 开始，后面全部重跑
     *
     * @param jobExecution 历史执行记录
     * @param allStepNames 所有 Step 名称（按顺序）
     * @return 需要重新执行的 Step 名称列表
     */
    public List<String> getStepsToResumeFrom(JobExecution jobExecution, List<String> allStepNames) {
        if (jobExecution == null || allStepNames == null || allStepNames.isEmpty()) {
            return allStepNames;
        }

        String firstFailedStep = getFirstFailedStepName(jobExecution);
        if (firstFailedStep == null) {
            // 没有失败的 Step，返回空列表
            return Collections.emptyList();
        }

        // 找到第一个失败 Step 的索引
        int startIndex = -1;
        for (int i = 0; i < allStepNames.size(); i++) {
            if (allStepNames.get(i).equals(firstFailedStep)) {
                startIndex = i;
                break;
            }
        }

        if (startIndex == -1) {
            // 找不到对应的 Step，返回全部
            log.warn("First failed step '{}' not found in step list, returning all steps", firstFailedStep);
            return allStepNames;
        }

        // 返回从第一个失败 Step 开始的所有后续 Step
        return allStepNames.subList(startIndex, allStepNames.size());
    }

    /**
     * 获取所有失败的 Step 名称
     *
     * @param jobExecution 历史执行记录
     * @return 失败的 Step 名称列表
     */
    public List<String> getFailedStepNames(JobExecution jobExecution) {
        if (jobExecution == null) {
            return Collections.emptyList();
        }

        return jobExecution.getStepExecutions().stream()
            .filter(step -> step.getStatus() == BatchStatus.FAILED)
            .sorted(Comparator.comparing(StepExecution::getStartTime))
            .map(StepExecution::getStepName)
            .toList();
    }

    /**
     * 获取所有已完成的 Step 名称
     *
     * @param jobExecution 历史执行记录
     * @return 已完成的 Step 名称列表
     */
    public List<String> getCompletedStepNames(JobExecution jobExecution) {
        if (jobExecution == null) {
            return Collections.emptyList();
        }

        return jobExecution.getStepExecutions().stream()
            .filter(step -> step.getStatus() == BatchStatus.COMPLETED)
            .sorted(Comparator.comparing(StepExecution::getStartTime))
            .map(StepExecution::getStepName)
            .toList();
    }

    /**
     * 构建执行状态摘要（用于日志输出）
     *
     * @param jobExecution 历史执行记录
     * @return 状态摘要字符串
     */
    public String buildStatusSummary(JobExecution jobExecution) {
        if (jobExecution == null) {
            return "No historical execution found";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Execution ID: %d, Status: %s%n",
            jobExecution.getId(), jobExecution.getStatus()));

        sb.append("Steps: ");
        for (StepExecution step : jobExecution.getStepExecutions()) {
            sb.append(String.format("[%s: %s] ", step.getStepName(), step.getStatus()));
        }

        return sb.toString();
    }
}
