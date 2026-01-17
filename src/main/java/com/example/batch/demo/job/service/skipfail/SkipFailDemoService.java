package com.example.batch.demo.job.service.skipfail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 容错执行演示 Job 业务服务（JdbcTemplate 优化方案）
 *
 * 设计目标：验证 SKIP_FAIL 模式下多数据源的部分失败容忍
 *
 * 数据插入规则：
 * - db1: 5 条 FETCHED 数据
 * - db2: 5 条 PARSED 数据
 * - db3: 5 条 VALIDATED 数据
 * - db4: 5 条 PERSISTED 数据
 *
 * 每个方法都支持失败模拟，用于测试 SKIP_FAIL 模式
 *
 * 优化点：
 * - 使用 NamedParameterJdbcTemplate 提高 SQL 可读性
 * - 使用批量插入提升性能
 * - 显式声明事务传播级别
 */
@Slf4j
@Service
public class SkipFailDemoService {

    private final NamedParameterJdbcTemplate namedJdbcTemplate1;
    private final NamedParameterJdbcTemplate namedJdbcTemplate2;
    private final NamedParameterJdbcTemplate namedJdbcTemplate3;
    private final NamedParameterJdbcTemplate namedJdbcTemplate4;

    public SkipFailDemoService(@Qualifier("namedJdbcTemplate1") NamedParameterJdbcTemplate namedJdbcTemplate1,
                               @Qualifier("namedJdbcTemplate2") NamedParameterJdbcTemplate namedJdbcTemplate2,
                               @Qualifier("namedJdbcTemplate3") NamedParameterJdbcTemplate namedJdbcTemplate3,
                               @Qualifier("namedJdbcTemplate4") NamedParameterJdbcTemplate namedJdbcTemplate4) {
        this.namedJdbcTemplate1 = namedJdbcTemplate1;
        this.namedJdbcTemplate2 = namedJdbcTemplate2;
        this.namedJdbcTemplate3 = namedJdbcTemplate3;
        this.namedJdbcTemplate4 = namedJdbcTemplate4;
    }

    /**
     * DB1 获取数据：批量插入 5 条 FETCHED 数据
     * 事务：参与 Step (tm1) 的事务
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm1")
    public void fetchDb1Data(boolean simulateFail) {
        log.info("[DB1] ========== 开始获取 DB1 数据 ==========");

        log.info("[DB1] 清空旧数据...");
        namedJdbcTemplate1.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB1] 批量插入 5 条 FETCHED 数据...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[5];
        for (int i = 0; i < 5; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "SkipFail-DB1-User-" + (i + 1))
                    .addValue("email", "skipfail.db1.user" + (i + 1) + "@example.com")
                    .addValue("status", "FETCHED");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate1.batchUpdate(sql, batchParams);

        if (simulateFail) {
            log.error("[DB1] 模拟失败！在 SKIP_FAIL 模式下将跳过此 Step");
            throw new RuntimeException("Simulated failure in DB1 Fetch Step (step1)");
        }

        log.info("[DB1] DB1 获取完成，批量插入 {} 条 FETCHED 记录", insertCounts.length);
        log.info("[DB1] ========== DB1 Step 完成 ==========\n");
    }

    /**
     * DB2 解析数据：批量插入 5 条 PARSED 数据
     * 事务：参与 Step (tm2) 的事务
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm2")
    public void parseDb2Data(boolean simulateFail) {
        log.info("[DB2] ========== 开始解析 DB2 数据 ==========");

        log.info("[DB2] 清空旧数据...");
        namedJdbcTemplate2.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB2] 批量插入 5 条 PARSED 数据...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[5];
        for (int i = 0; i < 5; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "SkipFail-DB2-User-" + (i + 1))
                    .addValue("email", "skipfail.db2.user" + (i + 1) + "@example.com")
                    .addValue("status", "PARSED");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate2.batchUpdate(sql, batchParams);

        if (simulateFail) {
            log.error("[DB2] 模拟失败！在 SKIP_FAIL 模式下将跳过此 Step");
            throw new RuntimeException("Simulated failure in DB2 Parse Step (step2)");
        }

        log.info("[DB2] DB2 解析完成，批量插入 {} 条 PARSED 记录", insertCounts.length);
        log.info("[DB2] ========== DB2 Step 完成 ==========\n");
    }

    /**
     * DB3 校验数据：批量插入 5 条 VALIDATED 数据
     * 事务：参与 Step (tm3) 的事务
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm3")
    public void validateDb3Data(boolean simulateFail) {
        log.info("[DB3] ========== 开始校验 DB3 数据 ==========");

        log.info("[DB3] 清空旧数据...");
        namedJdbcTemplate3.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB3] 批量插入 5 条 VALIDATED 数据...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[5];
        for (int i = 0; i < 5; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "SkipFail-DB3-User-" + (i + 1))
                    .addValue("email", "skipfail.db3.user" + (i + 1) + "@example.com")
                    .addValue("status", "VALIDATED");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate3.batchUpdate(sql, batchParams);

        if (simulateFail) {
            log.error("[DB3] 模拟失败！在 SKIP_FAIL 模式下将跳过此 Step");
            throw new RuntimeException("Simulated failure in DB3 Validate Step (step3)");
        }

        log.info("[DB3] DB3 校验完成，批量插入 {} 条 VALIDATED 记录", insertCounts.length);
        log.info("[DB3] ========== DB3 Step 完成 ==========\n");
    }

    /**
     * DB4 持久化：批量插入 5 条 PERSISTED 数据
     * 事务：参与 Step (tm4) 的事务
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm4")
    public void persistDb4Data(boolean simulateFail) {
        log.info("[DB4] ========== 开始持久化 DB4 数据 ==========");

        log.info("[DB4] 清空旧数据...");
        namedJdbcTemplate4.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB4] 批量插入 5 条 PERSISTED 数据...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[5];
        for (int i = 0; i < 5; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "SkipFail-DB4-User-" + (i + 1))
                    .addValue("email", "skipfail.db4.user" + (i + 1) + "@example.com")
                    .addValue("status", "PERSISTED");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate4.batchUpdate(sql, batchParams);

        if (simulateFail) {
            log.error("[DB4] 模拟失败！在 SKIP_FAIL 模式下将跳过此 Step");
            throw new RuntimeException("Simulated failure in DB4 Persist Step (step4)");
        }

        log.info("[DB4] DB4 持久化完成，批量插入 {} 条 PERSISTED 记录", insertCounts.length);
        log.info("[DB4] ========== DB4 Step 完成 ==========\n");
    }
}
