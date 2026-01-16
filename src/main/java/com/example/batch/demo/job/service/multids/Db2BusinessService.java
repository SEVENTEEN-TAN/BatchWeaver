package com.example.batch.demo.job.service.multids;

import com.example.batch.demo.infrastructure.entity.DemoUserEntity;
import com.example.batch.demo.infrastructure.mapper.Db2UserMapper;
import com.mybatisflex.annotation.UseDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DB2 业务服务
 * 负责 DB2_Business 的业务逻辑
 *
 * 场景：用户激活处理
 */
@Slf4j
@Service
public class Db2BusinessService {

    @Autowired
    private Db2UserMapper mapper;

    /**
     * 处理 DB2 数据：插入用户并激活
     *
     * @UseDataSource("db2") 确保在 db2 数据源上执行
     */
    @Transactional
    @UseDataSource("db2")
    public void processDb2Data() {
        log.info("[DB2] ========== 开始处理 DB2 数据 ==========");

        // 插入用户数据
        log.info("[DB2] 插入待激活用户...");
        for (int i = 1; i <= 3; i++) {
            DemoUserEntity user = new DemoUserEntity();
            user.setUsername("DB2-User-" + i);
            user.setEmail("db2user" + i + "@example.com");
            user.setStatus("PENDING");
            user.setUpdateTime(LocalDateTime.now());
            mapper.insert(user);
        }

        // 查询并激活
        log.info("[DB2] 查询待激活用户...");
        List<DemoUserEntity> pending = mapper.findPending();
        if (!pending.isEmpty()) {
            List<Long> ids = pending.stream().map(DemoUserEntity::getId).collect(Collectors.toList());
            int updated = mapper.bulkUpdateStatus(ids, "ACTIVE");
            log.info("[DB2] 已激活 {} 个用户", updated);
        }

        log.info("[DB2] ========== DB2 Step 完成 ==========\n");
    }
}
