package com.example.batch.demo.job.service.multids;

import com.example.batch.demo.infrastructure.entity.DemoUserEntity;
import com.example.batch.demo.infrastructure.mapper.DemoUserRowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 多数据源演示 Job 业务服务（JdbcTemplate 优化方案）
 *
 * 优化点：
 * 1. 使用 NamedParameterJdbcTemplate 提高 SQL 可读性
 * 2. 使用批量插入（batchUpdate）提升性能
 * 3. 显式声明事务传播级别（REQUIRED）
 * 4. 事务由调用方 Step 的 TransactionManager 管理
 *
 * 设计说明：
 * - 每个方法使用注入的 NamedParameterJdbcTemplate
 * - @Transactional(propagation = REQUIRED) 表示参与 Step 的事务
 * - 无需 @UseDataSource 注解，数据源在构造函数注入时确定
 * - 批量操作使用 SqlParameterSource[] 提升性能
 */
@Slf4j
@Service
public class MultiDsDemoService {

    private final NamedParameterJdbcTemplate namedJdbcTemplate1;
    private final NamedParameterJdbcTemplate namedJdbcTemplate2;
    private final NamedParameterJdbcTemplate namedJdbcTemplate3;
    private final NamedParameterJdbcTemplate namedJdbcTemplate4;

    public MultiDsDemoService(@Qualifier("namedJdbcTemplate1") NamedParameterJdbcTemplate namedJdbcTemplate1,
                              @Qualifier("namedJdbcTemplate2") NamedParameterJdbcTemplate namedJdbcTemplate2,
                              @Qualifier("namedJdbcTemplate3") NamedParameterJdbcTemplate namedJdbcTemplate3,
                              @Qualifier("namedJdbcTemplate4") NamedParameterJdbcTemplate namedJdbcTemplate4) {
        this.namedJdbcTemplate1 = namedJdbcTemplate1;
        this.namedJdbcTemplate2 = namedJdbcTemplate2;
        this.namedJdbcTemplate3 = namedJdbcTemplate3;
        this.namedJdbcTemplate4 = namedJdbcTemplate4;
    }

    /**
     * DB1 初始化：清空表 + 批量插入 10 条 PENDING 数据
     *
     * 优化：使用 batchUpdate + 命名参数
     * 事务：参与 Step (tm1) 的事务
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm1")
    public void initDb1Data() {
        log.info("[DB1] ========== 开始初始化 DB1 数据 ==========");

        log.info("[DB1] 清空旧数据...");
        namedJdbcTemplate1.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB1] 批量插入 10 条 PENDING 数据...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[10];
        for (int i = 0; i < 10; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "MultiDs-DB1-User-" + (i + 1))
                    .addValue("email", "multids.db1.user" + (i + 1) + "@example.com")
                    .addValue("status", "PENDING");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate1.batchUpdate(sql, batchParams);
        log.info("[DB1] DB1 初始化完成，批量插入 {} 条 PENDING 记录", insertCounts.length);
        log.info("[DB1] ========== DB1 Step 完成 ==========\n");
    }

    /**
     * DB2 处理：清空表 + 批量插入 8 条 PROCESSING 数据
     *
     * 优化：使用 batchUpdate + 命名参数
     * 事务：参与 Step (tm2) 的事务
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm2")
    public void processDb2Data(boolean simulateFail) {
        log.info("[DB2] ========== 开始处理 DB2 数据 ==========");

        log.info("[DB2] 清空旧数据...");
        namedJdbcTemplate2.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB2] 批量插入 8 条 PROCESSING 数据...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[8];
        for (int i = 0; i < 8; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "MultiDs-DB2-User-" + (i + 1))
                    .addValue("email", "multids.db2.user" + (i + 1) + "@example.com")
                    .addValue("status", "PROCESSING");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate2.batchUpdate(sql, batchParams);
        log.info("[DB2] 批量插入完成，共 {} 条记录", insertCounts.length);

        if (simulateFail) {
            log.error("[DB2] 模拟失败！事务将回滚，DB2 数据将被撤销");
            throw new RuntimeException("Simulated failure in DB2 Step (step2)");
        }

        log.info("[DB2] DB2 处理完成");
        log.info("[DB2] ========== DB2 Step 完成 ==========\n");
    }

    /**
     * DB3 转换：清空表 + 批量插入 6 条 TRANSFORMED 数据
     *
     * 优化：使用 batchUpdate + 命名参数
     * 事务：参与 Step (tm3) 的事务
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm3")
    public void transformDb3Data(boolean simulateFail) {
        log.info("[DB3] ========== 开始转换 DB3 数据 ==========");

        log.info("[DB3] 清空旧数据...");
        namedJdbcTemplate3.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB3] 批量插入 6 条 TRANSFORMED 数据...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[6];
        for (int i = 0; i < 6; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "MultiDs-DB3-User-" + (i + 1))
                    .addValue("email", "multids.db3.user" + (i + 1) + "@example.com")
                    .addValue("status", "TRANSFORMED");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate3.batchUpdate(sql, batchParams);
        log.info("[DB3] 批量插入完成，共 {} 条记录", insertCounts.length);

        if (simulateFail) {
            log.error("[DB3] 模拟失败！事务将回滚，DB3 数据将被撤销");
            throw new RuntimeException("Simulated failure in DB3 Step (step3)");
        }

        log.info("[DB3] DB3 转换完成");
        log.info("[DB3] ========== DB3 Step 完成 ==========\n");
    }

    /**
     * DB4 完成：清空表 + 批量插入 4 条 COMPLETED 数据
     *
     * 优化：使用 batchUpdate + 命名参数
     * 事务：参与 Step (tm4) 的事务
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm4")
    public void finalizeDb4Data(boolean simulateFail) {
        log.info("[DB4] ========== 开始完成 DB4 数据 ==========");

        log.info("[DB4] 清空旧数据...");
        namedJdbcTemplate4.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB4] 批量插入 4 条 COMPLETED 数据...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[4];
        for (int i = 0; i < 4; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "MultiDs-DB4-User-" + (i + 1))
                    .addValue("email", "multids.db4.user" + (i + 1) + "@example.com")
                    .addValue("status", "COMPLETED");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate4.batchUpdate(sql, batchParams);
        log.info("[DB4] 批量插入完成，共 {} 条记录", insertCounts.length);

        if (simulateFail) {
            log.error("[DB4] 模拟失败！事务将回滚，DB4 数据将被撤销");
            throw new RuntimeException("Simulated failure in DB4 Step (step4)");
        }

        log.info("[DB4] DB4 完成");
        log.info("[DB4] ========== DB4 Step 完成 ==========\n");
    }

    /**
     * 查询方法示例：查询 DB1 中的所有 PENDING 用户
     * 使用命名参数提高可读性
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm1", readOnly = true)
    public List<DemoUserEntity> findDb1PendingUsers() {
        String sql = "SELECT * FROM DEMO_USER WHERE STATUS = :status";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", "PENDING");
        return namedJdbcTemplate1.query(sql, params, DemoUserRowMapper.INSTANCE);
    }

    /**
     * 批量更新示例：更新 DB1 中指定 ID 列表的用户状态
     * 使用批量更新优化性能
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm1")
    public int[] bulkUpdateStatusInDb1(List<Long> ids, String status) {
        String sql = "UPDATE DEMO_USER SET STATUS = :status, UPDATE_TIME = GETDATE() WHERE ID = :id";

        List<SqlParameterSource> batchParams = new ArrayList<>(ids.size());
        for (Long id : ids) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("status", status)
                    .addValue("id", id);
            batchParams.add(params);
        }

        return namedJdbcTemplate1.batchUpdate(sql, batchParams.toArray(new SqlParameterSource[0]));
    }
}
