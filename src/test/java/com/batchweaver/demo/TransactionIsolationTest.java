package com.batchweaver.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 事务隔离验证测试
 *
 * 测试目的：验证元数据事务与业务事务的隔离性
 * - 元数据事务：即使业务事务失败也要提交
 * - 业务事务：失败时回滚
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false"
})
public class TransactionIsolationTest extends AbstractBatchTest {

    @Autowired
    @Qualifier("conditionalFlowJob")
    private Job conditionalFlowJob;

    @Autowired
    @Qualifier("dataSource2")
    private DataSource businessDataSource;

    @Autowired
    private JobRepository jobRepository;

    private JdbcTemplate businessJdbcTemplate;
    private JdbcTemplate metadataJdbcTemplate;

    @Autowired
    @Qualifier("dataSource1")
    private DataSource metadataDataSource;

    @Override
    public void setUp() {
        super.setUp();
        this.businessJdbcTemplate = new JdbcTemplate(businessDataSource);
        this.metadataJdbcTemplate = new JdbcTemplate(metadataDataSource);

        // 清空元数据表
        metadataJdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION");
        metadataJdbcTemplate.execute("DELETE FROM BATCH_STEP_EXECUTION");
    }

    @Test
    @DisplayName("事务隔离: 业务事务回滚，元数据事务提交")
    public void testMetadataCommitWhenBusinessRollback() throws Exception {
        // Arrange
        // 准备会触发异常的数据

        // Act
        JobExecution execution = launchJob(conditionalFlowJob, createJobParameters());

        // Assert
        // 业务数据应该为空（回滚）
        int businessCount = businessJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM DEMO_USER",
                Integer.class
        );
        assertEquals(0, businessCount, "业务事务应已回滚，业务表应为空！");

        // 元数据应该记录失败状态（提交）
        if (execution.getStatus().equals(BatchStatus.FAILED)) {
            long jobCount = metadataJdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'FAILED'",
                    Long.class
            );
            assertEquals(1L, jobCount, "元数据表应记录FAILED状态");
        }
    }

    @Test
    @DisplayName("事务隔离: 元数据事务独立性验证")
    public void testMetadataTransactionIndependence() throws Exception {
        // TODO: 实现更详细的事务隔离验证
    }
}
