package com.example.batch.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Spring Batch 配置类
 *
 * 核心功能：确保 Spring Batch 元数据操作使用独立的 DataSource 和 TransactionManager
 * 这样业务事务回滚时，不会影响 JobRepository 的元数据提交
 */
@Configuration
public class BatchConfig extends DefaultBatchConfiguration {

    /**
     * 为 Spring Batch 元数据创建独立的 DataSource
     * 复用 mybatis-flex.datasource.db1 的配置
     *
     * 注意：此 DataSource 不参与 MyBatis-Flex 的多数据源路由
     */
    @Bean("batchMetadataDataSource")
    @Primary
    @ConfigurationProperties("mybatis-flex.datasource.db1")
    public DataSource batchMetadataDataSource() {
        return new HikariDataSource();
    }

    /**
     * Spring Batch 元数据专用的事务管理器
     */
    @Bean("batchTransactionManager")
    public PlatformTransactionManager batchTransactionManager() {
        return new DataSourceTransactionManager(batchMetadataDataSource());
    }

    /**
     * 重写父类方法，强制 JobRepository 使用 batchMetadataDataSource
     * 确保元数据操作与业务事务完全隔离
     */
    @Override
    protected DataSource getDataSource() {
        return batchMetadataDataSource();
    }

    /**
     * 重写父类方法，强制 JobRepository 使用 batchTransactionManager
     */
    @Override
    protected PlatformTransactionManager getTransactionManager() {
        return batchTransactionManager();
    }
}
