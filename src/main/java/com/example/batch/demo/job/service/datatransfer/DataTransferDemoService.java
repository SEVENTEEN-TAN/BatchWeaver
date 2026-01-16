package com.example.batch.demo.job.service.datatransfer;

import com.example.batch.demo.infrastructure.entity.DemoUserEntity;
import com.example.batch.demo.infrastructure.mapper.Db1UserMapper;
import com.example.batch.demo.infrastructure.mapper.Db2UserMapper;
import com.mybatisflex.annotation.UseDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 跨库数据传递演示 Job 业务服务
 * 
 * 设计目标：验证 ExecutionContext 在跨数据源 Step 间的数据共享
 * 
 * 数据流向：
 * - extractUserIds(): 从 db1 查询 PENDING 用户，返回 ID 列表
 * - enrichUsers(): 根据 ID 列表在 db2 插入对应记录
 * - loadUsers(): 更新 db1 中用户状态为 ACTIVE
 */
@Slf4j
@Service
public class DataTransferDemoService {

    @Autowired
    private Db1UserMapper db1Mapper;

    @Autowired
    private Db2UserMapper db2Mapper;

    /**
     * 提取用户 ID 列表
     * 数据源：db1
     * 
     * 1. 清空旧数据
     * 2. 插入 5 条 PENDING 测试数据
     * 3. 查询并返回 PENDING 用户的 ID 列表
     * 
     * @return 用户 ID 列表（用于写入 ExecutionContext）
     */
    @Transactional
    @UseDataSource("db1")
    public List<Long> extractUserIds() {
        log.info("[DB1] ========== 开始提取用户数据 ==========");
        
        // 清空旧数据
        log.info("[DB1] 清空旧数据...");
        db1Mapper.truncate();
        
        // 插入 5 条 PENDING 测试数据
        log.info("[DB1] 插入 5 条 PENDING 测试数据...");
        List<Long> insertedIds = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("Transfer-DB1-User-" + i);
            user.setEmail("transfer.db1.user" + i + "@example.com");
            user.setStatus("PENDING");
            user.setUpdateTime(LocalDateTime.now());
            db1Mapper.insert(user);
            insertedIds.add(user.getId());
        }
        
        // 查询 PENDING 用户
        log.info("[DB1] 查询 PENDING 用户...");
        List<DemoUserEntity> pendingUsers = db1Mapper.findPending();
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
     * 
     * 根据 userIds 在 db2 插入对应的丰富记录
     * 
     * @param userIds 从 ExecutionContext 读取的用户 ID 列表
     * @param simulateFail 是否模拟失败
     * @return 实际处理的记录数
     */
    @Transactional
    @UseDataSource("db2")
    public int enrichUsers(List<Long> userIds, boolean simulateFail) {
        log.info("[DB2] ========== 开始丰富用户数据 ==========");
        log.info("[DB2] 接收到 {} 个用户 ID: {}", userIds.size(), userIds);
        
        // 清空旧数据
        log.info("[DB2] 清空旧数据...");
        db2Mapper.truncate();
        
        // 为每个 userId 插入丰富记录
        log.info("[DB2] 插入丰富记录...");
        int count = 0;
        for (Long userId : userIds) {
            DemoUserEntity enrichedUser = new DemoUserEntity();
            enrichedUser.setUsername("Enriched-From-DB1-" + userId);
            enrichedUser.setEmail("enriched.user" + userId + "@enriched.com");
            enrichedUser.setStatus("ENRICHED");
            enrichedUser.setUpdateTime(LocalDateTime.now());
            db2Mapper.insert(enrichedUser);
            count++;
        }
        
        // 失败模拟点
        if (simulateFail) {
            log.error("[DB2] 模拟失败！事务将回滚，DB2 数据将被撤销");
            log.error("[DB2] 注意：ExecutionContext 中的 userIds 已保存，可用于 RESUME");
            throw new RuntimeException("Simulated failure in DB2 Enrich Step (step2)");
        }
        
        log.info("[DB2] 丰富完成，已插入 {} 条 ENRICHED 记录", count);
        log.info("[DB2] ========== DB2 Step 完成 ==========\n");
        
        return count;
    }

    /**
     * 加载用户数据
     * 数据源：db1
     * 
     * 更新 db1 中指定用户的状态为 ACTIVE
     * 
     * @param userIds 从 ExecutionContext 读取的用户 ID 列表
     * @param enrichedCount 从 ExecutionContext 读取的丰富记录数（用于验证）
     */
    @Transactional
    @UseDataSource("db1")
    public void loadUsers(List<Long> userIds, int enrichedCount) {
        log.info("[DB1] ========== 开始加载用户数据 ==========");
        log.info("[DB1] 接收到 {} 个用户 ID: {}", userIds.size(), userIds);
        log.info("[DB1] 丰富记录数（来自 ExecutionContext）: {}", enrichedCount);
        
        // 验证数据一致性
        if (enrichedCount != userIds.size()) {
            log.warn("[DB1] 警告：丰富记录数 ({}) 与用户 ID 数 ({}) 不一致！", 
                    enrichedCount, userIds.size());
        }
        
        // 批量更新状态为 ACTIVE
        log.info("[DB1] 批量更新用户状态为 ACTIVE...");
        int updated = db1Mapper.bulkUpdateStatus(userIds, "ACTIVE");
        
        log.info("[DB1] 加载完成，已更新 {} 条记录状态为 ACTIVE", updated);
        log.info("[DB1] ========== DB1 Step 完成 ==========\n");
    }
}
