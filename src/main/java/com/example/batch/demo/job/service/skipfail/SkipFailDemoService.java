package com.example.batch.demo.job.service.skipfail;

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
 * 容错执行演示 Job 业务服务
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
 */
@Slf4j
@Service
public class SkipFailDemoService {

    @Autowired
    private Db1UserMapper db1Mapper;

    @Autowired
    private Db2UserMapper db2Mapper;

    @Autowired
    private Db3UserMapper db3Mapper;

    @Autowired
    private Db4UserMapper db4Mapper;

    /**
     * DB1 获取数据：插入 5 条 FETCHED 数据
     * 
     * @param simulateFail 是否模拟失败
     */
    @Transactional
    @UseDataSource("db1")
    public void fetchDb1Data(boolean simulateFail) {
        log.info("[DB1] ========== 开始获取 DB1 数据 ==========");
        
        // 清空旧数据
        log.info("[DB1] 清空旧数据...");
        db1Mapper.truncate();
        
        // 插入 5 条 FETCHED 数据
        log.info("[DB1] 插入 5 条 FETCHED 数据...");
        for (int i = 1; i <= 5; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("SkipFail-DB1-User-" + i);
            user.setEmail("skipfail.db1.user" + i + "@example.com");
            user.setStatus("FETCHED");
            user.setUpdateTime(LocalDateTime.now());
            db1Mapper.insert(user);
        }
        
        // 失败模拟点
        if (simulateFail) {
            log.error("[DB1] 模拟失败！在 SKIP_FAIL 模式下将跳过此 Step");
            throw new RuntimeException("Simulated failure in DB1 Fetch Step (step1)");
        }
        
        log.info("[DB1] DB1 获取完成，已插入 5 条 FETCHED 记录");
        log.info("[DB1] ========== DB1 Step 完成 ==========\n");
    }

    /**
     * DB2 解析数据：插入 5 条 PARSED 数据
     * 
     * @param simulateFail 是否模拟失败
     */
    @Transactional
    @UseDataSource("db2")
    public void parseDb2Data(boolean simulateFail) {
        log.info("[DB2] ========== 开始解析 DB2 数据 ==========");
        
        // 清空旧数据
        log.info("[DB2] 清空旧数据...");
        db2Mapper.truncate();
        
        // 插入 5 条 PARSED 数据
        log.info("[DB2] 插入 5 条 PARSED 数据...");
        for (int i = 1; i <= 5; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("SkipFail-DB2-User-" + i);
            user.setEmail("skipfail.db2.user" + i + "@example.com");
            user.setStatus("PARSED");
            user.setUpdateTime(LocalDateTime.now());
            db2Mapper.insert(user);
        }
        
        // 失败模拟点
        if (simulateFail) {
            log.error("[DB2] 模拟失败！在 SKIP_FAIL 模式下将跳过此 Step");
            throw new RuntimeException("Simulated failure in DB2 Parse Step (step2)");
        }
        
        log.info("[DB2] DB2 解析完成，已插入 5 条 PARSED 记录");
        log.info("[DB2] ========== DB2 Step 完成 ==========\n");
    }

    /**
     * DB3 校验数据：插入 5 条 VALIDATED 数据
     * 
     * @param simulateFail 是否模拟失败
     */
    @Transactional
    @UseDataSource("db3")
    public void validateDb3Data(boolean simulateFail) {
        log.info("[DB3] ========== 开始校验 DB3 数据 ==========");
        
        // 清空旧数据
        log.info("[DB3] 清空旧数据...");
        db3Mapper.truncate();
        
        // 插入 5 条 VALIDATED 数据
        log.info("[DB3] 插入 5 条 VALIDATED 数据...");
        for (int i = 1; i <= 5; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("SkipFail-DB3-User-" + i);
            user.setEmail("skipfail.db3.user" + i + "@example.com");
            user.setStatus("VALIDATED");
            user.setUpdateTime(LocalDateTime.now());
            db3Mapper.insert(user);
        }
        
        // 失败模拟点
        if (simulateFail) {
            log.error("[DB3] 模拟失败！在 SKIP_FAIL 模式下将跳过此 Step");
            throw new RuntimeException("Simulated failure in DB3 Validate Step (step3)");
        }
        
        log.info("[DB3] DB3 校验完成，已插入 5 条 VALIDATED 记录");
        log.info("[DB3] ========== DB3 Step 完成 ==========\n");
    }

    /**
     * DB4 持久化：插入 5 条 PERSISTED 数据
     * 
     * @param simulateFail 是否模拟失败
     */
    @Transactional
    @UseDataSource("db4")
    public void persistDb4Data(boolean simulateFail) {
        log.info("[DB4] ========== 开始持久化 DB4 数据 ==========");
        
        // 清空旧数据
        log.info("[DB4] 清空旧数据...");
        db4Mapper.truncate();
        
        // 插入 5 条 PERSISTED 数据
        log.info("[DB4] 插入 5 条 PERSISTED 数据...");
        for (int i = 1; i <= 5; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("SkipFail-DB4-User-" + i);
            user.setEmail("skipfail.db4.user" + i + "@example.com");
            user.setStatus("PERSISTED");
            user.setUpdateTime(LocalDateTime.now());
            db4Mapper.insert(user);
        }
        
        // 失败模拟点
        if (simulateFail) {
            log.error("[DB4] 模拟失败！在 SKIP_FAIL 模式下将跳过此 Step");
            throw new RuntimeException("Simulated failure in DB4 Persist Step (step4)");
        }
        
        log.info("[DB4] DB4 持久化完成，已插入 5 条 PERSISTED 记录");
        log.info("[DB4] ========== DB4 Step 完成 ==========\n");
    }
}
