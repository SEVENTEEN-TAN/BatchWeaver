package com.example.batch.demo.job.service.datatransfer;

import com.example.batch.demo.infrastructure.entity.DemoUserEntity;
import com.example.batch.demo.infrastructure.mapper.DemoUserRowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨库数据传递演示 Job 业务服务（JdbcTemplate 优化方案）
 *
 * 设计目标：验证 ExecutionContext 在跨数据源 Step 间的数据共享
 *
 * 数据流向：
 * - extractUserIds(): 从 db1 查询 PENDING 用户，返回 ID 列表
 * - enrichUsers(): 根据 ID 列表在 db2 插入对应记录
 * - loadUsers(): 更新 db1 中用户状态为 ACTIVE
 *
 * 优化点：
 * - 使用 NamedParameterJdbcTemplate 提高 SQL 可读性
 * - 使用批量插入提升性能
 * - 显式声明事务传播级别
 */
@Slf4j
@Service
public class DataTransferDemoService {

    private final NamedParameterJdbcTemplate namedJdbcTemplate1;
    private final NamedParameterJdbcTemplate namedJdbcTemplate2;

    public DataTransferDemoService(@Qualifier("namedJdbcTemplate1") NamedParameterJdbcTemplate namedJdbcTemplate1,
                                   @Qualifier("namedJdbcTemplate2") NamedParameterJdbcTemplate namedJdbcTemplate2) {
        this.namedJdbcTemplate1 = namedJdbcTemplate1;
        this.namedJdbcTemplate2 = namedJdbcTemplate2;
    }

    /**
     * 提取用户 ID 列表
     * 数据源：db1
     * 事务：参与 Step (tm1) 的事务
     *
     * 1. 清空旧数据
     * 2. 批量插入 5 条 PENDING 测试数据
     * 3. 查询并返回 PENDING 用户的 ID 列表
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm1")
    public List<Long> extractUserIds() {
        log.info("[DB1] ========== 开始提取用户数据 ==========");

        log.info("[DB1] 清空旧数据...");
        namedJdbcTemplate1.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB1] 批量插入 5 条 PENDING 测试数据...");
        String insertSql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                           "VALUES (:username, :email, :status, GETDATE())";

        List<Long> insertedIds = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "Transfer-DB1-User-" + i)
                    .addValue("email", "transfer.db1.user" + i + "@example.com")
                    .addValue("status", "PENDING");

            KeyHolder keyHolder = new GeneratedKeyHolder();
            namedJdbcTemplate1.update(insertSql, params, keyHolder);

            Number key = keyHolder.getKey();
            if (key != null) {
                insertedIds.add(key.longValue());
            }
        }

        log.info("[DB1] 查询 PENDING 用户...");
        String querySql = "SELECT * FROM DEMO_USER WHERE STATUS = :status";
        MapSqlParameterSource queryParams = new MapSqlParameterSource()
                .addValue("status", "PENDING");

        List<DemoUserEntity> pendingUsers = namedJdbcTemplate1.query(querySql, queryParams, DemoUserRowMapper.INSTANCE);

        List<Long> userIds = new ArrayList<>();
        for (DemoUserEntity user : pendingUsers) {
            userIds.add(user.getId());
        }

        log.info("[DB1] 提取完成，共 {} 条 PENDING 用户", userIds.size());
        log.info("[DB1] 用户 ID 列表: {}", userIds);
        log.info("[DB1] ========== DB1 Step 完成 ==========\n");

        return userIds;
    }

    /**
     * 丰富用户数据
     * 数据源：db2
     * 事务：参与 Step (tm2) 的事务
     *
     * 根据 userIds 在 db2 批量插入对应的丰富记录
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm2")
    public int enrichUsers(List<Long> userIds, boolean simulateFail) {
        log.info("[DB2] ========== 开始丰富用户数据 ==========");
        log.info("[DB2] 接收到 {} 个用户 ID: {}", userIds.size(), userIds);

        log.info("[DB2] 清空旧数据...");
        namedJdbcTemplate2.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB2] 批量插入丰富记录...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[userIds.size()];
        for (int i = 0; i < userIds.size(); i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "Enriched-From-DB1-" + userIds.get(i))
                    .addValue("email", "enriched.user" + userIds.get(i) + "@enriched.com")
                    .addValue("status", "ENRICHED");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate2.batchUpdate(sql, batchParams);

        if (simulateFail) {
            log.error("[DB2] 模拟失败！事务将回滚，DB2 数据将被撤销");
            log.error("[DB2] 注意：ExecutionContext 中的 userIds 已保存，可用于 RESUME");
            throw new RuntimeException("Simulated failure in DB2 Enrich Step (step2)");
        }

        log.info("[DB2] 丰富完成，已插入 {} 条 ENRICHED 记录", insertCounts.length);
        log.info("[DB2] ========== DB2 Step 完成 ==========\n");

        return insertCounts.length;
    }

    /**
     * 加载用户数据
     * 数据源：db1
     * 事务：参与 Step (tm1) 的事务
     *
     * 批量更新 db1 中指定用户的状态为 ACTIVE
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm1")
    public void loadUsers(List<Long> userIds, int enrichedCount) {
        log.info("[DB1] ========== 开始加载用户数据 ==========");
        log.info("[DB1] 接收到 {} 个用户 ID: {}", userIds.size(), userIds);
        log.info("[DB1] 丰富记录数（来自 ExecutionContext）: {}", enrichedCount);

        if (enrichedCount != userIds.size()) {
            log.warn("[DB1] 警告：丰富记录数 ({}) 与用户 ID 数 ({}) 不一致！",
                    enrichedCount, userIds.size());
        }

        log.info("[DB1] 批量更新用户状态为 ACTIVE...");
        String sql = "UPDATE DEMO_USER SET STATUS = :status, UPDATE_TIME = GETDATE() WHERE ID = :id";

        List<SqlParameterSource> batchParams = new ArrayList<>();
        for (Long id : userIds) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("status", "ACTIVE")
                    .addValue("id", id);
            batchParams.add(params);
        }

        int[] updateCounts = namedJdbcTemplate1.batchUpdate(sql, batchParams.toArray(new SqlParameterSource[0]));

        log.info("[DB1] 加载完成，已更新 {} 条记录状态为 ACTIVE", updateCounts.length);
        log.info("[DB1] ========== DB1 Step 完成 ==========\n");
    }
}
