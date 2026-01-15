package com.example.batch.job.service.multids;

import com.example.batch.infrastructure.entity.DemoUserEntity;
import com.example.batch.infrastructure.mapper.Db4UserMapper;
import com.mybatisflex.annotation.UseDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * DB4 业务服务
 * 负责 DB4_Business 的业务逻辑
 *
 * 场景：分析数据推送
 */
@Slf4j
@Service
public class Db4BusinessService {

    @Autowired
    private Db4UserMapper mapper;

    /**
     * 处理 DB4 数据：推送汇总数据
     *
     * @UseDataSource("db4") 确保在 db4 数据源上执行
     */
    @Transactional
    @UseDataSource("db4")
    public void processDb4Data() {
        log.info("[DB4] ========== 开始处理 DB4 数据 ==========");

        // 插入汇总数据
        log.info("[DB4] 推送分析汇总数据...");
        DemoUserEntity summary = new DemoUserEntity();
        summary.setUsername("Summary-" + System.currentTimeMillis());
        summary.setEmail("summary@example.com");
        summary.setStatus("COMPLETED");
        summary.setUpdateTime(LocalDateTime.now());
        mapper.insert(summary);

        log.info("[DB4] 汇总数据已推送，ID={}", summary.getId());
        log.info("[DB4] ========== DB4 Step 完成 ==========\n");
    }
}
