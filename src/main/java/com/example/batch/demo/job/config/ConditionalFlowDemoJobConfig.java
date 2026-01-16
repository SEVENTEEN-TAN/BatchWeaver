package com.example.batch.demo.job.config;

import com.example.batch.core.execution.BatchJob;
import com.example.batch.demo.job.service.conditionalflow.ConditionalFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 条件流演示 Job 配置
 * 
 * 设计目标：验证条件流的分支逻辑与执行模式的兼容性
 * 
 * 流程定义：
 * initStep ─┬─ [FAILED] → failurePathStep ─┐
 *           └─ [*] → successPathStep ───────┼─→ commonStep
 * 
 * Step 说明：
 * 1. initStep (db1)：插入初始数据 + 控制分支（参数 flow=success/fail）
 * 2. successPathStep (db1)：更新 STATUS=SUCCESS（flow=success 时执行）
 * 3. failurePathStep (db1)：更新 STATUS=FAILED_HANDLED（flow=fail 时执行）
 * 4. commonStep (db1)：汇聚点，统计最终状态
 * 
 * 约束：条件流不支持 SKIP_FAIL 模式
 */
@Slf4j
@Configuration
@BatchJob(name = "conditionalFlowDemoJob", 
          steps = {"initStep", "successPathStep", "failurePathStep", "commonStep"}, 
          conditionalFlow = true)
public class ConditionalFlowDemoJobConfig {

    @Bean
    public Job conditionalFlowDemoJob(JobRepository jobRepository,
                                      Step initStep,
                                      Step successPathStep,
                                      Step failurePathStep,
                                      Step commonStep) {
        return new JobBuilder("conditionalFlowDemoJob", jobRepository)
                .start(initStep)
                    .on("FAILED").to(failurePathStep)
                    .next(commonStep)
                .from(initStep)
                    .on("*").to(successPathStep)
                    .next(commonStep)
                .end()
                .build();
    }

    /**
     * Step 1: 初始化（分支控制点）
     * 数据源：db1 (BatchWeaverDB)
     * 业务：插入初始数据 + 根据参数控制分支
     * 
     * 参数：flow=success 走成功分支，flow=fail 走失败分支
     */
    @Bean
    public Step initStep(JobRepository jobRepository,
                         @Qualifier("businessTransactionManager") PlatformTransactionManager tx,
                         ConditionalFlowService service) {
        return new StepBuilder("initStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 1: 初始化（分支控制点） ====================");
                    
                    // 获取 flow 参数
                    String flow = (String) chunkContext.getStepContext()
                            .getJobParameters().get("flow");
                    
                    // 执行初始化
                    service.initData();
                    
                    // 根据参数决定分支
                    if ("fail".equalsIgnoreCase(flow)) {
                        log.info("[分支控制] flow=fail，将走失败分支 (failurePathStep)");
                        contribution.setExitStatus(ExitStatus.FAILED);
                    } else {
                        log.info("[分支控制] flow=success（或默认），将走成功分支 (successPathStep)");
                        contribution.setExitStatus(ExitStatus.COMPLETED);
                    }
                    
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }

    /**
     * Step 2: 成功分支
     * 数据源：db1 (BatchWeaverDB)
     * 业务：更新 STATUS=SUCCESS
     */
    @Bean
    public Step successPathStep(JobRepository jobRepository,
                                @Qualifier("businessTransactionManager") PlatformTransactionManager tx,
                                ConditionalFlowService service) {
        return new StepBuilder("successPathStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 2: 成功分支 ====================");
                    service.handleSuccess();
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }

    /**
     * Step 3: 失败分支
     * 数据源：db1 (BatchWeaverDB)
     * 业务：更新 STATUS=FAILED_HANDLED
     */
    @Bean
    public Step failurePathStep(JobRepository jobRepository,
                                @Qualifier("businessTransactionManager") PlatformTransactionManager tx,
                                ConditionalFlowService service) {
        return new StepBuilder("failurePathStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 3: 失败分支 ====================");
                    service.handleFailure();
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }

    /**
     * Step 4: 汇聚点
     * 数据源：db1 (BatchWeaverDB)
     * 业务：统计最终状态
     */
    @Bean
    public Step commonStep(JobRepository jobRepository,
                           @Qualifier("businessTransactionManager") PlatformTransactionManager tx,
                           ConditionalFlowService service) {
        return new StepBuilder("commonStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 4: 汇聚点 ====================");
                    service.summarize();
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }
}
