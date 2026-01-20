package com.batchweaver.demo;

import com.batchweaver.demo.config.ConditionalFlowConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Job1: 条件流程测试
 *
 * 测试目的：验证 Spring Batch 的条件分支功能
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false"
})
public class Job1ConditionalFlowTest extends AbstractBatchTest {

    @Autowired
    @Qualifier("conditionalFlowJob")
    private Job conditionalFlowJob;

    @Autowired
    private JobRepository jobRepository;

    @Test
    @DisplayName("Job1: 正常流程 - 成功分支")
    public void testConditionalFlow_Success() throws Exception {
        // Arrange
        // 确保测试数据文件存在且正确

        // Act
        JobExecution execution = launchJob(conditionalFlowJob, createJobParameters());

        // Assert
        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(3, getBusinessRecordCount());
    }

    @Test
    @DisplayName("Job1: 文件不存在 - 失败分支")
    public void testConditionalFlow_FileNotFound() throws Exception {
        // TODO: 实现文件不存在测试
    }

    @Test
    @DisplayName("Job1: 数据格式错误 - Skip 并成功")
    public void testConditionalFlow_SkipSuccess() throws Exception {
        // TODO: 实现 skip 测试
    }
}
