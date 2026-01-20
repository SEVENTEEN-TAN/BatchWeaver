package com.batchweaver.demo;

import com.batchweaver.demo.config.ChunkProcessingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Job2: 批处理模式测试
 *
 * 测试目的：验证基于 Chunk 的批处理模式
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false"
})
public class Job2ChunkProcessingTest extends AbstractBatchTest {

    @Autowired
    @Qualifier("chunkProcessingJob")
    private Job chunkProcessingJob;

    @Test
    @DisplayName("Job2: 小批量处理 (250条)")
    public void testChunkProcessing_SmallBatch() throws Exception {
        // Arrange
        // 准备包含 250 条记录的测试文件

        // Act
        JobExecution execution = launchJob(chunkProcessingJob, createJobParameters());

        // Assert
        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(250, getBusinessRecordCount());
    }

    @Test
    @DisplayName("Job2: Chunk 提交次数验证")
    public void testChunkProcessing_ChunkCommitCount() throws Exception {
        // TODO: 验证 Chunk 提交次数
    }
}
