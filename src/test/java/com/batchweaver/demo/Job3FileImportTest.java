package com.batchweaver.demo;

import com.batchweaver.demo.config.FileImportConfig;
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
 * Job3: 文件导入测试
 *
 * 测试目的：验证不同格式的文件导入功能
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false"
})
public class Job3FileImportTest extends AbstractBatchTest {

    @Autowired
    @Qualifier("format1ImportJob")
    private Job format1ImportJob;

    @Autowired
    @Qualifier("format2ImportJob")
    private Job format2ImportJob;

    @Autowired
    @Qualifier("format3ImportJob")
    private Job format3ImportJob;

    @Test
    @DisplayName("Job3: 格式1导入 - yyyyMMdd + 纯数字")
    public void testFormat1Import() throws Exception {
        // Arrange
        // 确保 format1_users.txt 存在

        // Act
        JobExecution execution = launchJob(format1ImportJob, createJobParameters());

        // Assert
        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(3, getBusinessRecordCount());
    }

    @Test
    @DisplayName("Job3: 格式2导入 - MMddyyyy + R前缀")
    public void testFormat2Import() throws Exception {
        // Act
        JobExecution execution = launchJob(format2ImportJob, createJobParameters());

        // Assert
        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(3, getBusinessRecordCount());
    }

    @Test
    @DisplayName("Job3: 格式3导入 - 无头尾")
    public void testFormat3Import() throws Exception {
        // Act
        JobExecution execution = launchJob(format3ImportJob, createJobParameters());

        // Assert
        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(3, getBusinessRecordCount());
    }

    @Test
    @DisplayName("Job3: Header 日期不匹配 - 失败")
    public void testFormat1Import_HeaderDateMismatch() throws Exception {
        // TODO: 实现日期不匹配测试
    }

    @Test
    @DisplayName("Job3: Footer 数量不匹配 - 失败")
    public void testFormat1Import_FooterCountMismatch() throws Exception {
        // TODO: 实现数量不匹配测试
    }
}
