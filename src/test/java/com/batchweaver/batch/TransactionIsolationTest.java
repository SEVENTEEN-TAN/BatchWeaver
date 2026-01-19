package com.batchweaver.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事务隔离验证测试（核心测试）
 *
 * 测试目标：验证元数据事务（tm1）独立性，确保业务失败时元数据仍能提交
 *
 * 测试场景：
 * 1. 正常流程：文件导入成功，元数据记录 COMPLETED
 * 2. 异常流程：业务失败（如唯一约束冲突），元数据记录 FAILED，业务数据回滚
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional(transactionManager = "tm2", propagation = Propagation.NOT_SUPPORTED)  // 禁用测试默认事务
public class TransactionIsolationTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job demoJob;

    @Autowired
    @Qualifier("jdbcTemplate1")
    private JdbcTemplate jdbcTemplate1;  // 元数据库（db1）

    @Autowired
    @Qualifier("jdbcTemplate2")
    private JdbcTemplate jdbcTemplate2;  // 业务库（db2）

    /**
     * 测试 1：正常流程 - Job 成功执行
     */
    @Test
    public void testJobSuccess() throws Exception {
        // 1. 准备：清空业务表和元数据表
        cleanupTables();

        // 2. 执行：运行 Job
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", "data/input/demo_users.txt")
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(demoJob, jobParameters);

        // 3. 验证：Job 执行状态为 COMPLETED
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // 4. 验证：元数据表已记录 COMPLETED 状态
        Long jobExecutionCount = jdbcTemplate1.queryForObject(
            "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'COMPLETED'",
            Long.class
        );
        assertEquals(1L, jobExecutionCount);

        // 5. 验证：业务表有数据（3 条记录）
        Long businessDataCount = jdbcTemplate2.queryForObject(
            "SELECT COUNT(*) FROM DEMO_USER",
            Long.class
        );
        assertEquals(3L, businessDataCount);
    }

    /**
     * 测试 2：异常流程 - 业务失败，元数据仍提交
     *
     * 核心测试：验证事务隔离机制
     * - 业务事务（tm2）回滚 → 业务表为空
     * - 元数据事务（tm1）提交 → 元数据表记录 FAILED 状态
     */
    @Test
    public void testMetadataCommitWhenBusinessRollback() throws Exception {
        // 1. 准备：清空业务表和元数据表
        cleanupTables();

        // 2. 准备：插入冲突数据（id=1），导致后续插入失败
        jdbcTemplate2.execute(
            "INSERT INTO DEMO_USER (id, name, email, birth_date) " +
            "VALUES (1, 'Existing User', 'existing@example.com', '2000-01-01')"
        );

        // 3. 执行：运行 Job（预期失败，因为文件中也有 id=1）
        JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", "data/input/demo_users.txt")
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        JobExecution jobExecution = jobLauncher.run(demoJob, jobParameters);

        // 4. 验证：Job 执行状态为 FAILED
        assertEquals(BatchStatus.FAILED, jobExecution.getStatus());

        // 5. ✅ 关键验证：元数据表已记录 FAILED 状态（tm1 提交成功）
        Long jobExecutionCount = jdbcTemplate1.queryForObject(
            "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION WHERE STATUS = 'FAILED'",
            Long.class
        );
        assertEquals(1L, jobExecutionCount, "元数据表应记录 FAILED 状态");

        Long stepExecutionCount = jdbcTemplate1.queryForObject(
            "SELECT COUNT(*) FROM BATCH_STEP_EXECUTION WHERE STATUS = 'FAILED'",
            Long.class
        );
        assertEquals(1L, stepExecutionCount, "Step 元数据应记录 FAILED 状态");

        // 6. ✅ 关键验证：业务表只有原有数据（1 条），新数据已回滚（tm2 回滚成功）
        Long businessDataCount = jdbcTemplate2.queryForObject(
            "SELECT COUNT(*) FROM DEMO_USER",
            Long.class
        );
        assertEquals(1L, businessDataCount, "业务事务应已回滚，业务表应只有原有的 1 条数据");

        // 7. 验证：重跑机制可用（元数据记录了失败状态，支持断点续传）
        assertNotNull(jobExecution);
        assertNotNull(jobExecution.getId());
    }

    /**
     * 清空测试表
     */
    private void cleanupTables() {
        // 清空业务表
        jdbcTemplate2.execute("DELETE FROM DEMO_USER");

        // 清空元数据表（谨慎操作，仅测试环境）
        jdbcTemplate1.execute("DELETE FROM BATCH_STEP_EXECUTION_CONTEXT");
        jdbcTemplate1.execute("DELETE FROM BATCH_JOB_EXECUTION_CONTEXT");
        jdbcTemplate1.execute("DELETE FROM BATCH_STEP_EXECUTION");
        jdbcTemplate1.execute("DELETE FROM BATCH_JOB_EXECUTION_PARAMS");
        jdbcTemplate1.execute("DELETE FROM BATCH_JOB_EXECUTION");
        jdbcTemplate1.execute("DELETE FROM BATCH_JOB_INSTANCE");
    }
}
