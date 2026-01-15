package com.example.batch.job.service.multids;

import com.example.batch.infrastructure.entity.DemoUserEntity;
import com.example.batch.infrastructure.mapper.Db1UserMapper;
import com.mybatisflex.annotation.UseDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * DB1 业务服务
 * 负责 DB1（BatchWeaverDB）的业务逻辑
 *
 * 场景：元数据同步 + 初始化数据
 */
@Slf4j
@Service
public class Db1BusinessService {

    @Autowired
    private Db1UserMapper mapper;

    /**
     * 处理 DB1 数据：初始化用户数据
     *
     * @UseDataSource("db1") 确保在 db1 数据源上执行
     * @Transactional 由 businessTransactionManager 管理事务
     */
    @Transactional
    @UseDataSource("db1")
    public void processDb1Data() {
        log.info("[DB1] ========== 开始处理 DB1 数据 ==========");

        // 清空旧数据（Demo 用途）
        log.info("[DB1] 清空旧数据...");
        mapper.truncate();

        // 插入初始数据
        log.info("[DB1] 插入初始用户数据...");
        for (int i = 1; i <= 5; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("DB1-User-" + i);
            user.setEmail("db1user" + i + "@example.com");
            user.setStatus("PENDING");
            user.setUpdateTime(LocalDateTime.now());
            mapper.insert(user);
        }

        log.info("[DB1] DB1 数据处理完成，已插入 5 条记录");
        log.info("[DB1] ========== DB1 Step 完成 ==========\n");
    }
}
