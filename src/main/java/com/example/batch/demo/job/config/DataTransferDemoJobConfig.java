package com.example.batch.demo.job.config;

import com.example.batch.core.execution.BatchJob;
import com.example.batch.demo.job.service.datatransfer.DataTransferDemoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * 跨库数据传递演示 Job 配置
 * 
 * 设计目标：验证 ExecutionContext 在跨数据源 Step 间的数据共享
 * 
 * Step 流程：
 * 1. extractStep (db1)：查询 PENDING 用户 ID 列表，写入 ExecutionContext
 * 2. enrichStep (db2)：读取 userIds，在 db2 插入对应记录
 * 3. loadStep (db1)：读取 enrichedCount，更新 db1 用户状态为 ACTIVE
 * 
 * 核心验证：
 * - List<Long> 序列化跨 Step 传递
 * - 跨数据源操作的上下文一致性
 * - RESUME 模式下历史上下文的恢复
 */
@Slf4j
@Configuration
@BatchJob(name = "dataTransferDemoJob", steps = {"extractStep", "enrichStep", "loadStep"})
public class DataTransferDemoJobConfig {

    @Bean
    public Job dataTransferDemoJob(JobRepository jobRepository,
                                   Step extractStep,
                                   Step enrichStep,
                                   Step loadStep) {
        return new JobBuilder("dataTransferDemoJob", jobRepository)
                .start(extractStep)
                .next(enrichStep)
                .next(loadStep)
                .build();
    }

    /**
     * Step 1: 提取数据
     * 数据源：db1 (BatchWeaverDB)
     * 业务：初始化数据 + 查询 PENDING 用户 ID 列表
     * ExecutionContext：写入 userIds (List<Long>)
     */
    @Bean
    public Step extractStep(JobRepository jobRepository,
                            @Qualifier("tm1") PlatformTransactionManager tm1,
                            DataTransferDemoService service) {
        return new StepBuilder("extractStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 1: 提取数据 (DB1) ====================");
                    
                    // 获取 Job ExecutionContext
                    ExecutionContext ctx = chunkContext.getStepContext()
                            .getStepExecution().getJobExecution().getExecutionContext();
                    
                    // 提取用户 ID 列表并存入上下文
                    List<Long> userIds = service.extractUserIds();
                    ctx.put("userIds", userIds);
                    ctx.putInt("extractCount", userIds.size());
                    
                    log.info("[ExecutionContext] 已写入 userIds: {}", userIds);
                    log.info("[ExecutionContext] 已写入 extractCount: {}", userIds.size());

                    return RepeatStatus.FINISHED;
                }, tm1)  // 使用 tm1
                .build();
    }

    /**
     * Step 2: 丰富数据
     * 数据源：db2 (DB2_Business)
     * 业务：读取 userIds，在 db2 插入对应记录
     * ExecutionContext：读取 userIds，写入 enrichedCount
     * 失败模拟：simulateFail=step2
     */
    @Bean
    @SuppressWarnings("unchecked")
    public Step enrichStep(JobRepository jobRepository,
                           @Qualifier("tm2") PlatformTransactionManager tm2,
                           DataTransferDemoService service) {
        return new StepBuilder("enrichStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 2: 丰富数据 (DB2) ====================");
                    
                    // 获取 Job ExecutionContext
                    ExecutionContext ctx = chunkContext.getStepContext()
                            .getStepExecution().getJobExecution().getExecutionContext();
                    
                    // 从上下文读取 userIds
                    List<Long> userIds = (List<Long>) ctx.get("userIds");
                    if (userIds == null || userIds.isEmpty()) {
                        log.warn("[ExecutionContext] userIds 为空，跳过丰富操作");
                        ctx.putInt("enrichedCount", 0);
                        return RepeatStatus.FINISHED;
                    }
                    
                    log.info("[ExecutionContext] 读取到 userIds: {}", userIds);
                    
                    // 检查是否需要模拟失败
                    String simulateFail = (String) chunkContext.getStepContext()
                            .getJobParameters().get("simulateFail");
                    boolean shouldFail = "step2".equalsIgnoreCase(simulateFail);
                    
                    // 丰富数据并获取处理数量
                    int enrichedCount = service.enrichUsers(userIds, shouldFail);
                    ctx.putInt("enrichedCount", enrichedCount);
                    
                    log.info("[ExecutionContext] 已写入 enrichedCount: {}", enrichedCount);

                    return RepeatStatus.FINISHED;
                }, tm2)  // 使用 tm2
                .build();
    }

    /**
     * Step 3: 加载数据
     * 数据源：db1 (BatchWeaverDB)
     * 业务：读取 enrichedCount，更新 db1 用户状态为 ACTIVE
     * ExecutionContext：读取 enrichedCount 进行验证
     */
    @Bean
    @SuppressWarnings("unchecked")
    public Step loadStep(JobRepository jobRepository,
                         @Qualifier("tm1") PlatformTransactionManager tm1,
                         DataTransferDemoService service) {
        return new StepBuilder("loadStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("\n==================== Step 3: 加载数据 (DB1) ====================");
                    
                    // 获取 Job ExecutionContext
                    ExecutionContext ctx = chunkContext.getStepContext()
                            .getStepExecution().getJobExecution().getExecutionContext();
                    
                    // 从上下文读取数据
                    List<Long> userIds = (List<Long>) ctx.get("userIds");
                    int enrichedCount = ctx.getInt("enrichedCount", 0);
                    
                    log.info("[ExecutionContext] 读取到 userIds: {}", userIds);
                    log.info("[ExecutionContext] 读取到 enrichedCount: {}", enrichedCount);
                    
                    if (userIds == null || userIds.isEmpty()) {
                        log.warn("[ExecutionContext] userIds 为空，跳过加载操作");
                        return RepeatStatus.FINISHED;
                    }
                    
                    // 加载数据（更新状态）
                    service.loadUsers(userIds, enrichedCount);

                    return RepeatStatus.FINISHED;
                }, tm1)  // 使用 tm1
                .build();
    }
}
