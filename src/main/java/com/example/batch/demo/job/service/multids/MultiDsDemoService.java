package com.example.batch.demo.job.service.multids;

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
 * 多数据源演示 Job 业务服务
 * 
 * 每个方法操作不同的数据源，通过 @UseDataSource 注解切换
 * 每个方法独立事务，失败时仅回滚当前数据源
 * 
 * 数据插入规则：
 * - db1: 10 条 PENDING 数据
 * - db2: 8 条 PROCESSING 数据
 * - db3: 6 条 TRANSFORMED 数据
 * - db4: 4 条 COMPLETED 数据
 */
@Slf4j
@Service
public class MultiDsDemoService {

    @Autowired
    private Db1UserMapper db1Mapper;

    @Autowired
    private Db2UserMapper db2Mapper;

    @Autowired
    private Db3UserMapper db3Mapper;

    @Autowired
    private Db4UserMapper db4Mapper;

    /**
     * DB1 初始化：清空表 + 插入 10 条 PENDING 数据
     */
    @Transactional
    @UseDataSource("db1")
    public void initDb1Data() {
        log.info("[DB1] ========== 开始初始化 DB1 数据 ==========");
        
        // 清空旧数据
        log.info("[DB1] 清空旧数据...");
        db1Mapper.truncate();
        
        // 插入 10 条 PENDING 数据
        log.info("[DB1] 插入 10 条 PENDING 数据...");
        for (int i = 1; i <= 10; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("MultiDs-DB1-User-" + i);
            user.setEmail("multids.db1.user" + i + "@example.com");
            user.setStatus("PENDING");
            user.setUpdateTime(LocalDateTime.now());
            db1Mapper.insert(user);
        }
        
        log.info("[DB1] DB1 初始化完成，已插入 10 条 PENDING 记录");
        log.info("[DB1] ========== DB1 Step 完成 ==========\n");
    }

    /**
     * DB2 处理：清空表 + 插入 8 条 PROCESSING 数据
     * 
     * @param simulateFail 是否模拟失败
     */
    @Transactional
    @UseDataSource("db2")
    public void processDb2Data(boolean simulateFail) {
        log.info("[DB2] ========== 开始处理 DB2 数据 ==========");
        
        // 清空旧数据
        log.info("[DB2] 清空旧数据...");
        db2Mapper.truncate();
        
        // 插入 8 条 PROCESSING 数据
        log.info("[DB2] 插入 8 条 PROCESSING 数据...");
        for (int i = 1; i <= 8; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("MultiDs-DB2-User-" + i);
            user.setEmail("multids.db2.user" + i + "@example.com");
            user.setStatus("PROCESSING");
            user.setUpdateTime(LocalDateTime.now());
            db2Mapper.insert(user);
        }
        
        // 失败模拟点
        if (simulateFail) {
            log.error("[DB2] 模拟失败！事务将回滚，DB2 数据将被撤销");
            throw new RuntimeException("Simulated failure in DB2 Step (step2)");
        }
        
        log.info("[DB2] DB2 处理完成，已插入 8 条 PROCESSING 记录");
        log.info("[DB2] ========== DB2 Step 完成 ==========\n");
    }

    /**
     * DB3 转换：清空表 + 插入 6 条 TRANSFORMED 数据
     * 
     * @param simulateFail 是否模拟失败
     */
    @Transactional
    @UseDataSource("db3")
    public void transformDb3Data(boolean simulateFail) {
        log.info("[DB3] ========== 开始转换 DB3 数据 ==========");
        
        // 清空旧数据
        log.info("[DB3] 清空旧数据...");
        db3Mapper.truncate();
        
        // 插入 6 条 TRANSFORMED 数据
        log.info("[DB3] 插入 6 条 TRANSFORMED 数据...");
        for (int i = 1; i <= 6; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("MultiDs-DB3-User-" + i);
            user.setEmail("multids.db3.user" + i + "@example.com");
            user.setStatus("TRANSFORMED");
            user.setUpdateTime(LocalDateTime.now());
            db3Mapper.insert(user);
        }
        
        // 失败模拟点
        if (simulateFail) {
            log.error("[DB3] 模拟失败！事务将回滚，DB3 数据将被撤销");
            throw new RuntimeException("Simulated failure in DB3 Step (step3)");
        }
        
        log.info("[DB3] DB3 转换完成，已插入 6 条 TRANSFORMED 记录");
        log.info("[DB3] ========== DB3 Step 完成 ==========\n");
    }

    /**
     * DB4 完成：清空表 + 插入 4 条 COMPLETED 数据
     * 
     * @param simulateFail 是否模拟失败
     */
    @Transactional
    @UseDataSource("db4")
    public void finalizeDb4Data(boolean simulateFail) {
        log.info("[DB4] ========== 开始完成 DB4 数据 ==========");
        
        // 清空旧数据
        log.info("[DB4] 清空旧数据...");
        db4Mapper.truncate();
        
        // 插入 4 条 COMPLETED 数据
        log.info("[DB4] 插入 4 条 COMPLETED 数据...");
        for (int i = 1; i <= 4; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("MultiDs-DB4-User-" + i);
            user.setEmail("multids.db4.user" + i + "@example.com");
            user.setStatus("COMPLETED");
            user.setUpdateTime(LocalDateTime.now());
            db4Mapper.insert(user);
        }
        
        // 失败模拟点
        if (simulateFail) {
            log.error("[DB4] 模拟失败！事务将回滚，DB4 数据将被撤销");
            throw new RuntimeException("Simulated failure in DB4 Step (step4)");
        }
        
        log.info("[DB4] DB4 完成，已插入 4 条 COMPLETED 记录");
        log.info("[DB4] ========== DB4 Step 完成 ==========\n");
    }
}
