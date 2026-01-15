package com.example.batch.job.service.multids;

import com.example.batch.infrastructure.entity.DemoUserEntity;
import com.example.batch.infrastructure.mapper.Db3UserMapper;
import com.mybatisflex.annotation.UseDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * DB3 业务服务
 * 负责 DB3_Business 的业务逻辑
 *
 * 场景：发票对账处理（包含失败注入点）
 */
@Slf4j
@Service
public class Db3BusinessService {

    @Autowired
    private Db3UserMapper mapper;

    /**
     * 处理 DB3 数据：发票对账（可模拟失败）
     *
     * @param simulateFail 是否模拟失败
     * @UseDataSource("db3") 确保在 db3 数据源上执行
     *
     * 失败场景：
     * 1. 插入发票记录
     * 2. 抛出异常
     * 3. 事务回滚（db3 数据撤销）
     * 4. Spring Batch 元数据记录失败状态（db1 独立提交）
     */
    @Transactional
    @UseDataSource("db3")
    public void processDb3Data(boolean simulateFail) {
        log.info("[DB3] ========== 开始处理 DB3 数据 ==========");
        log.info("[DB3] 模拟失败标志：{}", simulateFail);

        // 插入发票数据
        log.info("[DB3] 插入发票记录...");
        DemoUserEntity invoice = new DemoUserEntity();
        invoice.setUsername("Invoice-" + System.currentTimeMillis());
        invoice.setEmail("invoice@example.com");
        invoice.setStatus("PENDING");
        invoice.setUpdateTime(LocalDateTime.now());
        mapper.insert(invoice);
        log.info("[DB3] 已插入发票记录 ID={}", invoice.getId());

        // 模拟失败（用于测试断点续传）
        if (simulateFail) {
            log.error("[DB3] ⚠️ 模拟失败！抛出异常...");
            log.error("[DB3] ⚠️ 预期行为：DB3 事务回滚，元数据记录 FAILED 状态");
            throw new IllegalStateException("Simulated DB3 failure for testing - 模拟 DB3 故障");
        }

        // 更新状态为已对账
        invoice.setStatus("RECONCILED");
        mapper.update(invoice);
        log.info("[DB3] 发票对账完成，状态更新为 RECONCILED");

        log.info("[DB3] ========== DB3 Step 完成 ==========\n");
    }
}
