package com.example.batch.demo.job.service.failurerecovery;

import com.example.batch.demo.infrastructure.entity.DemoUserEntity;
import com.example.batch.demo.infrastructure.mapper.Db1UserMapper;
import com.example.batch.demo.infrastructure.mapper.Db2UserMapper;
import com.example.batch.demo.infrastructure.mapper.Db3UserMapper;
import com.example.batch.demo.infrastructure.mapper.Db4UserMapper;
import com.mybatisflex.annotation.UseDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 失败恢复演示 Job 业务服务
 * 
 * 设计目标：验证多数据源场景下的事务回滚和断点续传
 * 
 * 数据插入规则：
 * - db1: 5 条记录 (ID:1001-1005)，使用幂等操作
 * - db2: 5 条 PROCESSING 记录
 * - db3: 5 条记录，可模拟失败触发回滚
 * - db4: 5 条 COMPLETED 记录
 */
@Slf4j
@Service
public class FailureRecoveryDemoService {

    @Autowired
    private Db1UserMapper db1Mapper;

    @Autowired
    private Db2UserMapper db2Mapper;

    @Autowired
    private Db3UserMapper db3Mapper;

    @Autowired
    private Db4UserMapper db4Mapper;

    /**
     * DB1 准备：幂等插入 5 条记录 (ID:1001-1005)
     * 使用 MERGE 语句保证幂等性，重复执行不会报错
     */
    @Transactional
    @UseDataSource("db1")
    public void prepareDb1Data() {
        log.info("[DB1] ========== 开始准备 DB1 数据（幂等操作） ==========");
        
        // 先清空旧数据（Demo 用途）
        log.info("[DB1] 清空旧数据...");
        db1Mapper.truncate();
        
        // 使用普通 INSERT（清空后插入是幂等的）
        log.info("[DB1] 插入 5 条固定 ID 记录 (模拟幂等场景)...");
        for (int i = 1; i <= 5; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("Recovery-DB1-User-" + (1000 + i));
            user.setEmail("recovery.db1.user" + (1000 + i) + "@example.com");
            user.setStatus("PREPARED");
            user.setUpdateTime(LocalDateTime.now());
            db1Mapper.insert(user);
        }
        
        log.info("[DB1] DB1 准备完成，已插入 5 条 PREPARED 记录");
        log.info("[DB1] ========== DB1 Step 完成 ==========\n");
    }

    /**
     * DB2 更新：普通插入 5 条 PROCESSING 记录
     */
    @Transactional
    @UseDataSource("db2")
    public void updateDb2Data() {
        log.info("[DB2] ========== 开始更新 DB2 数据 ==========");
        
        // 清空旧数据
        log.info("[DB2] 清空旧数据...");
        db2Mapper.truncate();
        
        // 插入 5 条 PROCESSING 数据
        log.info("[DB2] 插入 5 条 PROCESSING 数据...");
        for (int i = 1; i <= 5; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("Recovery-DB2-User-" + i);
            user.setEmail("recovery.db2.user" + i + "@example.com");
            user.setStatus("PROCESSING");
            user.setUpdateTime(LocalDateTime.now());
            db2Mapper.insert(user);
        }
        
        log.info("[DB2] DB2 更新完成，已插入 5 条 PROCESSING 记录");
        log.info("[DB2] ========== DB2 Step 完成 ==========\n");
    }

    /**
     * DB3 风险操作：插入后可模拟失败
     * 
     * @param simulateFail 是否模拟失败（失败时事务回滚，DB3 数据撤销）
     */
    @Transactional
    @UseDataSource("db3")
    public void riskyDb3Operation(boolean simulateFail) {
        log.info("[DB3] ========== 开始 DB3 风险操作 ==========");
        
        // 清空旧数据
        log.info("[DB3] 清空旧数据...");
        db3Mapper.truncate();
        
        // 插入 5 条数据
        log.info("[DB3] 插入 5 条 RISKY 数据...");
        for (int i = 1; i <= 5; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("Recovery-DB3-User-" + i);
            user.setEmail("recovery.db3.user" + i + "@example.com");
            user.setStatus("RISKY");
            user.setUpdateTime(LocalDateTime.now());
            db3Mapper.insert(user);
        }
        
        // 失败模拟点（在数据插入后触发，验证回滚）
        if (simulateFail) {
            log.error("[DB3] 模拟失败！事务将回滚，DB3 数据将被撤销");
            log.error("[DB3] 预期结果：DB3 表应为空（0 条记录）");
            throw new RuntimeException("Simulated failure in DB3 Step (step3) - Transaction will rollback");
        }
        
        log.info("[DB3] DB3 风险操作完成，已插入 5 条 RISKY 记录");
        log.info("[DB3] ========== DB3 Step 完成 ==========\n");
    }

    /**
     * DB4 完成：插入最终 COMPLETED 记录
     */
    @Transactional
    @UseDataSource("db4")
    public void completeDb4Data() {
        log.info("[DB4] ========== 开始完成 DB4 数据 ==========");
        
        // 清空旧数据
        log.info("[DB4] 清空旧数据...");
        db4Mapper.truncate();
        
        // 插入 5 条 COMPLETED 数据
        log.info("[DB4] 插入 5 条 COMPLETED 数据...");
        for (int i = 1; i <= 5; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("Recovery-DB4-User-" + i);
            user.setEmail("recovery.db4.user" + i + "@example.com");
            user.setStatus("COMPLETED");
            user.setUpdateTime(LocalDateTime.now());
            db4Mapper.insert(user);
        }
        
        log.info("[DB4] DB4 完成，已插入 5 条 COMPLETED 记录");
        log.info("[DB4] ========== DB4 Step 完成 ==========\n");
    }
}
