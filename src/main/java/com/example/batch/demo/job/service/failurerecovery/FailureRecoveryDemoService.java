package com.example.batch.demo.job.service.failurerecovery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 失败恢复演示 Job 业务服务（JdbcTemplate 优化方案）
 *
 * 设计目标：验证多数据源场景下的事务回滚和断点续传
 *
 * 数据插入规则：
 * - db1: 5 条记录，使用幂等操作
 * - db2: 5 条 PROCESSING 记录
 * - db3: 5 条记录，可模拟失败触发回滚
 * - db4: 5 条 COMPLETED 记录
 *
 * 优化点：
 * - 使用 NamedParameterJdbcTemplate 提高 SQL 可读性
 * - 使用批量插入提升性能
 * - 显式声明事务传播级别
 */
@Slf4j
@Service
public class FailureRecoveryDemoService {

    private final NamedParameterJdbcTemplate namedJdbcTemplate1;
    private final NamedParameterJdbcTemplate namedJdbcTemplate2;
    private final NamedParameterJdbcTemplate namedJdbcTemplate3;
    private final NamedParameterJdbcTemplate namedJdbcTemplate4;

    public FailureRecoveryDemoService(@Qualifier("namedJdbcTemplate1") NamedParameterJdbcTemplate namedJdbcTemplate1,
                                      @Qualifier("namedJdbcTemplate2") NamedParameterJdbcTemplate namedJdbcTemplate2,
                                      @Qualifier("namedJdbcTemplate3") NamedParameterJdbcTemplate namedJdbcTemplate3,
                                      @Qualifier("namedJdbcTemplate4") NamedParameterJdbcTemplate namedJdbcTemplate4) {
        this.namedJdbcTemplate1 = namedJdbcTemplate1;
        this.namedJdbcTemplate2 = namedJdbcTemplate2;
        this.namedJdbcTemplate3 = namedJdbcTemplate3;
        this.namedJdbcTemplate4 = namedJdbcTemplate4;
    }

    /**
     * DB1 准备：批量插入 5 条记录（幂等操作）
     * 事务：参与 Step (tm1) 的事务
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm1")
    public void prepareDb1Data() {
        log.info("[DB1] ========== 开始准备 DB1 数据（幂等操作） ==========");

        log.info("[DB1] 清空旧数据...");
        namedJdbcTemplate1.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB1] 批量插入 5 条固定 ID 记录 (模拟幂等场景)...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[5];
        for (int i = 0; i < 5; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "Recovery-DB1-User-" + (1001 + i))
                    .addValue("email", "recovery.db1.user" + (1001 + i) + "@example.com")
                    .addValue("status", "PREPARED");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate1.batchUpdate(sql, batchParams);
        log.info("[DB1] DB1 准备完成，批量插入 {} 条 PREPARED 记录", insertCounts.length);
        log.info("[DB1] ========== DB1 Step 完成 ==========\n");
    }

    /**
     * DB2 更新：批量插入 5 条 PROCESSING 记录
     * 事务：参与 Step (tm2) 的事务
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm2")
    public void updateDb2Data() {
        log.info("[DB2] ========== 开始更新 DB2 数据 ==========");

        log.info("[DB2] 清空旧数据...");
        namedJdbcTemplate2.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB2] 批量插入 5 条 PROCESSING 数据...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[5];
        for (int i = 0; i < 5; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "Recovery-DB2-User-" + (i + 1))
                    .addValue("email", "recovery.db2.user" + (i + 1) + "@example.com")
                    .addValue("status", "PROCESSING");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate2.batchUpdate(sql, batchParams);
        log.info("[DB2] DB2 更新完成，批量插入 {} 条 PROCESSING 记录", insertCounts.length);
        log.info("[DB2] ========== DB2 Step 完成 ==========\n");
    }

    /**
     * DB3 风险操作：批量插入后可模拟失败
     * 事务：参与 Step (tm3) 的事务
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm3")
    public void riskyDb3Operation(boolean simulateFail) {
        log.info("[DB3] ========== 开始 DB3 风险操作 ==========");

        log.info("[DB3] 清空旧数据...");
        namedJdbcTemplate3.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB3] 批量插入 5 条 RISKY 数据...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[5];
        for (int i = 0; i < 5; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "Recovery-DB3-User-" + (i + 1))
                    .addValue("email", "recovery.db3.user" + (i + 1) + "@example.com")
                    .addValue("status", "RISKY");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate3.batchUpdate(sql, batchParams);
        log.info("[DB3] 批量插入完成，共 {} 条记录", insertCounts.length);

        if (simulateFail) {
            log.error("[DB3] 模拟失败！事务将回滚，DB3 数据将被撤销");
            log.error("[DB3] 预期结果：DB3 表应为空（0 条记录）");
            throw new RuntimeException("Simulated failure in DB3 Step (step3) - Transaction will rollback");
        }

        log.info("[DB3] DB3 风险操作完成，已插入 5 条 RISKY 记录");
        log.info("[DB3] ========== DB3 Step 完成 ==========\n");
    }

    /**
     * DB4 完成：批量插入最终 COMPLETED 记录
     * 事务：参与 Step (tm4) 的事务
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm4")
    public void completeDb4Data() {
        log.info("[DB4] ========== 开始完成 DB4 数据 ==========");

        log.info("[DB4] 清空旧数据...");
        namedJdbcTemplate4.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB4] 批量插入 5 条 COMPLETED 数据...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[5];
        for (int i = 0; i < 5; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "Recovery-DB4-User-" + (i + 1))
                    .addValue("email", "recovery.db4.user" + (i + 1) + "@example.com")
                    .addValue("status", "COMPLETED");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate4.batchUpdate(sql, batchParams);
        log.info("[DB4] DB4 完成，批量插入 {} 条 COMPLETED 记录", insertCounts.length);
        log.info("[DB4] ========== DB4 Step 完成 ==========\n");
    }
}
