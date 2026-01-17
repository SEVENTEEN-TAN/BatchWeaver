package com.example.batch.demo.job.service.conditionalflow;

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
import java.util.Map;

/**
 * 条件流演示 Job 业务服务（JdbcTemplate 优化方案）
 *
 * 设计目标：验证条件流的分支逻辑
 *
 * 所有操作都在 db1 上执行，通过状态变化验证分支路径
 *
 * 优化点：
 * - 使用 NamedParameterJdbcTemplate 提高 SQL 可读性
 * - 使用批量插入/更新提升性能
 * - 显式声明事务传播级别
 */
@Slf4j
@Service
public class ConditionalFlowService {

    private final NamedParameterJdbcTemplate namedJdbcTemplate1;

    public ConditionalFlowService(@Qualifier("namedJdbcTemplate1") NamedParameterJdbcTemplate namedJdbcTemplate1) {
        this.namedJdbcTemplate1 = namedJdbcTemplate1;
    }

    /**
     * 初始化数据
     * 数据源：db1
     * 事务：参与 Step (tm1) 的事务
     *
     * 清空旧数据并批量插入 5 条 INIT 状态记录
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm1")
    public void initData() {
        log.info("[DB1] ========== 开始初始化数据 ==========");

        log.info("[DB1] 清空旧数据...");
        namedJdbcTemplate1.getJdbcTemplate().execute("TRUNCATE TABLE DEMO_USER");

        log.info("[DB1] 批量插入 5 条 INIT 数据...");
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        SqlParameterSource[] batchParams = new SqlParameterSource[5];
        for (int i = 0; i < 5; i++) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", "Conditional-User-" + (i + 1))
                    .addValue("email", "conditional.user" + (i + 1) + "@example.com")
                    .addValue("status", "INIT");
            batchParams[i] = params;
        }

        int[] insertCounts = namedJdbcTemplate1.batchUpdate(sql, batchParams);
        log.info("[DB1] 初始化完成，批量插入 {} 条 INIT 记录", insertCounts.length);
        log.info("[DB1] ========== 初始化 Step 完成 ==========\n");
    }

    /**
     * 处理成功分支
     * 数据源：db1
     * 事务：参与 Step (tm1) 的事务
     *
     * 批量将所有 INIT 状态更新为 SUCCESS
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm1")
    public void handleSuccess() {
        log.info("[DB1] ========== 成功分支处理 ==========");

        // 查询 INIT 状态的用户
        String querySql = "SELECT * FROM DEMO_USER WHERE STATUS = :status";
        MapSqlParameterSource queryParams = new MapSqlParameterSource()
                .addValue("status", "INIT");

        List<DemoUserEntity> users = namedJdbcTemplate1.query(querySql, queryParams, DemoUserRowMapper.INSTANCE);
        List<Long> userIds = new ArrayList<>();
        for (DemoUserEntity user : users) {
            userIds.add(user.getId());
        }

        if (!userIds.isEmpty()) {
            // 批量更新状态
            String updateSql = "UPDATE DEMO_USER SET STATUS = :status, UPDATE_TIME = GETDATE() WHERE ID = :id";

            List<SqlParameterSource> batchParams = new ArrayList<>();
            for (Long id : userIds) {
                MapSqlParameterSource params = new MapSqlParameterSource()
                        .addValue("status", "SUCCESS")
                        .addValue("id", id);
                batchParams.add(params);
            }

            int[] updateCounts = namedJdbcTemplate1.batchUpdate(updateSql, batchParams.toArray(new SqlParameterSource[0]));
            log.info("[DB1] 已批量更新 {} 条记录状态为 SUCCESS", updateCounts.length);
        } else {
            log.info("[DB1] 没有 INIT 状态的记录需要更新");
        }

        log.info("[DB1] ========== 成功分支 Step 完成 ==========\n");
    }

    /**
     * 处理失败分支
     * 数据源：db1
     * 事务：参与 Step (tm1) 的事务
     *
     * 批量将所有 INIT 状态更新为 FAILED_HANDLED
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm1")
    public void handleFailure() {
        log.info("[DB1] ========== 失败分支处理 ==========");

        // 查询 INIT 状态的用户
        String querySql = "SELECT * FROM DEMO_USER WHERE STATUS = :status";
        MapSqlParameterSource queryParams = new MapSqlParameterSource()
                .addValue("status", "INIT");

        List<DemoUserEntity> users = namedJdbcTemplate1.query(querySql, queryParams, DemoUserRowMapper.INSTANCE);
        List<Long> userIds = new ArrayList<>();
        for (DemoUserEntity user : users) {
            userIds.add(user.getId());
        }

        if (!userIds.isEmpty()) {
            // 批量更新状态
            String updateSql = "UPDATE DEMO_USER SET STATUS = :status, UPDATE_TIME = GETDATE() WHERE ID = :id";

            List<SqlParameterSource> batchParams = new ArrayList<>();
            for (Long id : userIds) {
                MapSqlParameterSource params = new MapSqlParameterSource()
                        .addValue("status", "FAILED_HANDLED")
                        .addValue("id", id);
                batchParams.add(params);
            }

            int[] updateCounts = namedJdbcTemplate1.batchUpdate(updateSql, batchParams.toArray(new SqlParameterSource[0]));
            log.info("[DB1] 已批量更新 {} 条记录状态为 FAILED_HANDLED", updateCounts.length);
        } else {
            log.info("[DB1] 没有 INIT 状态的记录需要更新");
        }

        log.info("[DB1] ========== 失败分支 Step 完成 ==========\n");
    }

    /**
     * 汇总统计
     * 数据源：db1
     * 事务：参与 Step (tm1) 的事务（只读操作）
     *
     * 输出各状态的记录数
     */
    @Transactional(propagation = Propagation.REQUIRED, transactionManager = "tm1", readOnly = true)
    public void summarize() {
        log.info("[DB1] ========== 汇总统计 ==========");

        // 统计各状态的记录数
        String sql = "SELECT STATUS, COUNT(*) as CNT FROM DEMO_USER GROUP BY STATUS";
        List<Map<String, Object>> results = namedJdbcTemplate1.getJdbcTemplate().queryForList(sql);

        log.info("[DB1] 数据状态统计：");
        for (Map<String, Object> row : results) {
            log.info("  - {}: {} 条", row.get("STATUS"), row.get("CNT"));
        }

        // 统计总数
        Integer total = namedJdbcTemplate1.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM DEMO_USER", Integer.class);
        log.info("[DB1] 总记录数: {}", total);

        log.info("[DB1] ========== 汇总 Step 完成 ==========\n");
    }
}
