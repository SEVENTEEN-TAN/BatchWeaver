package com.batchweaver.demo.components;

import com.batchweaver.core.fileprocess.function.PostImportValidator;
import com.batchweaver.core.fileprocess.listener.FooterValidationListener;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 共享 Listener 配置
 * <p>
 * 包含多个 Job 共用的监听器 Bean
 *
 * @author BatchWeaver Team
 * @since 1.0.0
 */
@Configuration
public class SharedListenersConfig {

    /**
     * Chunk 执行监听器
     * <p>
     * 在每个 Chunk 执行前后打印日志
     */
    @Bean
    public ChunkListener chunkExecutionListener() {
        return new ChunkListener() {
            @Override
            public void beforeChunk(ChunkContext context) {
                System.out.println("========================================");
                System.out.println("[CHUNK] Starting chunk: " + context.getAttribute("chunk.count"));
            }

            @Override
            public void afterChunk(ChunkContext context) {
                System.out.println("[CHUNK] Chunk completed");
                System.out.println("========================================");
            }
        };
    }

    /**
     * Step 执行监听器
     * <p>
     * 在 Step 执行前后打印统计信息
     */
    @Bean
    public StepExecutionListener StepExecutionListenerImpl() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                System.out.println("[CHUNK] Step started");
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                System.out.println("[CHUNK] Step completed. Read: " + stepExecution.getReadCount()
                        + ", Written: " + stepExecution.getWriteCount()
                        + ", Skipped: " + stepExecution.getSkipCount());
                return stepExecution.getExitStatus();
            }
        };
    }

    /**
     * Footer 校验监听器
     * <p>
     * 在导入完成后校验写入条数是否与 Footer 声明的条数一致
     * <p>
     * 适用场景：
     * - 有头有尾：严格校验 writeCount == declaredCount
     * - 有头无尾/无头无尾：declaredCount=0，可以选择跳过校验或使用 Job 参数
     */
    @Bean
    public FooterValidationListener footerValidationListener() {
        // 定义校验逻辑
        PostImportValidator validator = (declaredCount, readCount, writeCount, skipCount) -> {
            // 场景1：有Footer，严格校验
            if (declaredCount > 0) {
                if (writeCount != declaredCount) {
                    throw new IllegalStateException(
                            String.format("Import validation failed: Footer expects %d records, " +
                                    "but actually wrote %d (read=%d, skip=%d)",
                                    declaredCount, writeCount, readCount, skipCount)
                    );
                }
                System.out.println("✅ Import validation passed: Footer=" + declaredCount +
                        ", wrote=" + writeCount);
            } else {
                // 场景2/3：无Footer，记录日志但不失败
                System.out.println("ℹ️ No footer declaration, skipping count validation. " +
                        "Wrote " + writeCount + " records (read=" + readCount + ", skip=" + skipCount + ")");
            }
        };

        return new FooterValidationListener(validator);
    }
}
