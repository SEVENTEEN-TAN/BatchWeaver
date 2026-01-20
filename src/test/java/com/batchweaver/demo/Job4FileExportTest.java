package com.batchweaver.demo;

import com.batchweaver.demo.config.FileExportConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Job4: 文件导出测试
 *
 * 测试目的：验证数据从数据库导出到文件的功能
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false"
})
public class Job4FileExportTest extends AbstractBatchTest {

    @Autowired
    @Qualifier("format1ExportJob")
    private Job format1ExportJob;

    @Autowired
    @Qualifier("format2ExportJob")
    private Job format2ExportJob;

    private static final String OUTPUT_DIR = "data/output/";

    @BeforeEach
    public void setUpData() throws Exception {
        // 插入测试数据
        businessJdbcTemplate.update(
            "INSERT INTO DEMO_USER (id, name, email, birth_date) VALUES (?, ?, ?, ?)",
            1, "张三", "zhangsan@example.com", java.sql.Date.valueOf("1990-01-15")
        );
        businessJdbcTemplate.update(
            "INSERT INTO DEMO_USER (id, name, email, birth_date) VALUES (?, ?, ?, ?)",
            2, "李四", "lisi@example.com", java.sql.Date.valueOf("1985-06-20")
        );
        businessJdbcTemplate.update(
            "INSERT INTO DEMO_USER (id, name, email, birth_date) VALUES (?, ?, ?, ?)",
            3, "王五", "wangwu@example.com", java.sql.Date.valueOf("1987-03-10")
        );
    }

    @AfterEach
    public void tearDown() {
        // 清理输出文件
        try {
            Files.deleteIfExists(Path.of(OUTPUT_DIR + "format1_export.txt"));
            Files.deleteIfExists(Path.of(OUTPUT_DIR + "format2_export.txt"));
        } catch (Exception e) {
            // Ignore
        }
    }

    @Test
    @DisplayName("Job4: 格式1导出")
    public void testFormat1Export() throws Exception {
        // Act
        JobExecution execution = launchJob(format1ExportJob, createJobParameters());

        // Assert
        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, execution.getStatus());

        // 验证文件存在
        File file = new File(OUTPUT_DIR + "format1_export.txt");
        assertTrue(file.exists(), "导出文件应该存在");

        // TODO: 验证文件内容
    }

    @Test
    @DisplayName("Job4: 格式2导出")
    public void testFormat2Export() throws Exception {
        // Act
        JobExecution execution = launchJob(format2ExportJob, createJobParameters());

        // Assert
        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, execution.getStatus());

        // 验证文件存在
        File file = new File(OUTPUT_DIR + "format2_export.txt");
        assertTrue(file.exists(), "导出文件应该存在");
    }

    @Test
    @DisplayName("Job4: 空数据导出")
    public void testExportEmptyData() throws Exception {
        // 先清空数据
        businessJdbcTemplate.execute("DELETE FROM DEMO_USER");

        // Act
        JobExecution execution = launchJob(format1ExportJob, createJobParameters());

        // Assert
        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, execution.getStatus());
    }
}
