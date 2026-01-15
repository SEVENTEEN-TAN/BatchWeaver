package com.example.batch.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.datasource.FlexDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 多数据源配置类
 *
 * 核心功能：为业务逻辑提供基于 MyBatis-Flex FlexDataSource 的事务管理器
 * 配合 @UseDataSource 注解实现动态数据源路由
 */
@Slf4j
@Configuration
public class DataSourceConfig implements ApplicationListener<ContextRefreshedEvent> {

    private volatile PlatformTransactionManager businessTransactionManagerInstance;

    /**
     * 监听应用上下文刷新事件
     * 等待 MyBatis-Flex 完全初始化后，获取 FlexDataSource
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (businessTransactionManagerInstance == null) {
            synchronized (this) {
                if (businessTransactionManagerInstance == null) {
                    try {
                        FlexDataSource flexDataSource = FlexGlobalConfig.getDefaultConfig().getDataSource();
                        if (flexDataSource != null) {
                            businessTransactionManagerInstance = new DataSourceTransactionManager(flexDataSource);
                            log.info("业务事务管理器初始化成功，基于 FlexDataSource");
                        } else {
                            log.warn("FlexDataSource 尚未初始化，稍后重试");
                        }
                    } catch (Exception e) {
                        log.error("初始化业务事务管理器失败", e);
                    }
                }
            }
        }
    }

    /**
     * 业务逻辑专用的事务管理器
     * 基于 MyBatis-Flex 的 FlexDataSource
     *
     * 工作原理：
     * 1. Step 开始时，通过此 TransactionManager 开启事务
     * 2. Service 层方法执行时，@UseDataSource 切换到指定数据源
     * 3. FlexDataSource 根据当前路由返回对应的数据库连接
     * 4. 事务提交/回滚在正确的数据源上执行
     */
    @Bean("businessTransactionManager")
    public PlatformTransactionManager businessTransactionManager() {
        // 返回懒加载代理，避免启动时 FlexDataSource 未初始化
        return new LazyTransactionManagerProxy();
    }

    /**
     * 懒加载事务管理器代理
     * 在实际调用时才获取真正的 TransactionManager
     */
    private class LazyTransactionManagerProxy implements PlatformTransactionManager {

        private PlatformTransactionManager getDelegate() {
            if (businessTransactionManagerInstance == null) {
                throw new IllegalStateException(
                        "业务事务管理器尚未初始化，请确保 MyBatis-Flex 已完全启动");
            }
            return businessTransactionManagerInstance;
        }

        @Override
        public org.springframework.transaction.TransactionStatus getTransaction(
                org.springframework.transaction.TransactionDefinition definition)
                throws org.springframework.transaction.TransactionException {
            return getDelegate().getTransaction(definition);
        }

        @Override
        public void commit(org.springframework.transaction.TransactionStatus status)
                throws org.springframework.transaction.TransactionException {
            getDelegate().commit(status);
        }

        @Override
        public void rollback(org.springframework.transaction.TransactionStatus status)
                throws org.springframework.transaction.TransactionException {
            getDelegate().rollback(status);
        }
    }
}
