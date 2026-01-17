package com.example.batch.config;

import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Spring Batch 元数据配置
 *
 * 核心功能：
 * 1. 指定 Spring Batch 元数据（JobRepository）使用 ds1/tm1
 * 2. 确保元数据与业务事务完全隔离
 *
 * 工作原理：
 * - JobRepository 的所有操作（保存 JobInstance, StepExecution 等）使用 tm1
 * - 当 Step 执行失败时，tm1 会以 REQUIRES_NEW 传播级别单独提交元数据
 * - 这样即使业务事务回滚，元数据仍能正确记录失败状态
 */
@Configuration
public class BatchConfig extends DefaultBatchConfiguration {

    private final DataSource ds1;
    private final PlatformTransactionManager tm1;

    /**
     * 构造函数注入 ds1 和 tm1
     */
    public BatchConfig(@Qualifier("ds1") DataSource ds1,
                      @Qualifier("tm1") PlatformTransactionManager tm1) {
        this.ds1 = ds1;
        this.tm1 = tm1;
    }

    /**
     * 重写父类方法，指定 JobRepository 使用 ds1
     */
    @Override
    protected DataSource getDataSource() {
        return ds1;
    }

    /**
     * 重写父类方法，指定 JobRepository 使用 tm1
     */
    @Override
    protected PlatformTransactionManager getTransactionManager() {
        return tm1;
    }
}
