package com.batchweaver.core.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * DataSource1 配置 - db1（元数据 + 业务）
 * <p>
 * 🔴 关键数据源：承载 Spring Batch 元数据表 + 业务数据
 * - 元数据事务管理器：tm1（绑定 JobRepository，确保元数据事务独立）
 * - 业务数据：可选，也可以只在 db2/db3/db4 存储业务数据
 */
@Configuration
public class DataSource1Config {

    /**
     * db1 数据源（主数据源）
     * 用于 Spring Batch 元数据表 + 业务数据
     */
    @Primary
    @Bean(name = "dataSource1")
    @ConfigurationProperties(prefix = "spring.datasource.db1")
    public DataSource dataSource1() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * db1 JdbcTemplate
     */
    @Primary
    @Bean(name = "jdbcTemplate1")
    public JdbcTemplate jdbcTemplate1(@Qualifier("dataSource1") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * db1 NamedParameterJdbcTemplate
     */
    @Primary
    @Bean(name = "namedJdbcTemplate1")
    public NamedParameterJdbcTemplate namedJdbcTemplate1(@Qualifier("dataSource1") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * 🔴 tm1 - 元数据事务管理器
     * 用于管理 Spring Batch 元数据表的事务，确保元数据事务独立于业务事务
     * <p>
     * 关键：JobRepository 必须绑定此事务管理器，确保即使业务失败，元数据也能提交
     */
    @Primary
    @Bean(name = "tm1")
    public PlatformTransactionManager tm1(@Qualifier("dataSource1") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
