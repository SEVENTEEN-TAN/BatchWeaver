package com.example.batch.demo.job.service.conditionalflow;

import com.example.batch.demo.infrastructure.entity.DemoUserEntity;
import com.example.batch.demo.infrastructure.mapper.Db1UserMapper;
import com.mybatisflex.annotation.UseDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 条件流演示 Job 业务服务
 * 
 * 设计目标：验证条件流的分支逻辑
 * 
 * 所有操作都在 db1 上执行，通过状态变化验证分支路径
 */
@Slf4j
@Service
public class ConditionalFlowService {

    @Autowired
    private Db1UserMapper db1Mapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 初始化数据
     * 数据源：db1
     * 
     * 清空旧数据并插入 5 条 INIT 状态记录
     */
    @Transactional
    @UseDataSource("db1")
    public void initData() {
        log.info("[DB1] ========== 开始初始化数据 ==========");
        
        // 清空旧数据
        log.info("[DB1] 清空旧数据...");
        db1Mapper.truncate();
        
        // 插入 5 条 INIT 数据
        log.info("[DB1] 插入 5 条 INIT 数据...");
        for (int i = 1; i <= 5; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("Conditional-User-" + i);
            user.setEmail("conditional.user" + i + "@example.com");
            user.setStatus("INIT");
            user.setUpdateTime(LocalDateTime.now());
            db1Mapper.insert(user);
        }
        
        log.info("[DB1] 初始化完成，已插入 5 条 INIT 记录");
        log.info("[DB1] ========== 初始化 Step 完成 ==========\n");
    }

    /**
     * 处理成功分支
     * 数据源：db1
     * 
     * 将所有 INIT 状态更新为 SUCCESS
     */
    @Transactional
    @UseDataSource("db1")
    public void handleSuccess() {
        log.info("[DB1] ========== 成功分支处理 ==========");
        
        // 查询 INIT 状态的用户
        List<DemoUserEntity> users = db1Mapper.selectAll();
        List<Long> userIds = users.stream()
                .filter(u -> "INIT".equals(u.getStatus()))
                .map(DemoUserEntity::getId)
                .toList();
        
        if (!userIds.isEmpty()) {
            int updated = db1Mapper.bulkUpdateStatus(userIds, "SUCCESS");
            log.info("[DB1] 已将 {} 条记录状态更新为 SUCCESS", updated);
        } else {
            log.info("[DB1] 没有 INIT 状态的记录需要更新");
        }
        
        log.info("[DB1] ========== 成功分支 Step 完成 ==========\n");
    }

    /**
     * 处理失败分支
     * 数据源：db1
     * 
     * 将所有 INIT 状态更新为 FAILED_HANDLED
     */
    @Transactional
    @UseDataSource("db1")
    public void handleFailure() {
        log.info("[DB1] ========== 失败分支处理 ==========");
        
        // 查询 INIT 状态的用户
        List<DemoUserEntity> users = db1Mapper.selectAll();
        List<Long> userIds = users.stream()
                .filter(u -> "INIT".equals(u.getStatus()))
                .map(DemoUserEntity::getId)
                .toList();
        
        if (!userIds.isEmpty()) {
            int updated = db1Mapper.bulkUpdateStatus(userIds, "FAILED_HANDLED");
            log.info("[DB1] 已将 {} 条记录状态更新为 FAILED_HANDLED", updated);
        } else {
            log.info("[DB1] 没有 INIT 状态的记录需要更新");
        }
        
        log.info("[DB1] ========== 失败分支 Step 完成 ==========\n");
    }

    /**
     * 汇总统计
     * 数据源：db1
     * 
     * 输出各状态的记录数
     */
    @Transactional(readOnly = true)
    @UseDataSource("db1")
    public void summarize() {
        log.info("[DB1] ========== 汇总统计 ==========");
        
        // 统计各状态的记录数
        String sql = "SELECT STATUS, COUNT(*) as CNT FROM DEMO_USER GROUP BY STATUS";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
        
        log.info("[DB1] 数据状态统计：");
        for (Map<String, Object> row : results) {
            log.info("  - {}: {} 条", row.get("STATUS"), row.get("CNT"));
        }
        
        // 统计总数
        int total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM DEMO_USER", Integer.class);
        log.info("[DB1] 总记录数: {}", total);
        
        log.info("[DB1] ========== 汇总 Step 完成 ==========\n");
    }
}
