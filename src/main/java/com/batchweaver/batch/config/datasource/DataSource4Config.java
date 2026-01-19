package com.batchweaver.batch.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * DataSource4 配置 - db4（纯业务数据库）
 *
 * 业务事务管理器：tm4（用于 Step 的业务数据操作）
 */
@Configuration
public class DataSource4Config {

    @Bean(name = "dataSource4")
    @ConfigurationProperties(prefix = "spring.datasource.db4")
    public DataSource dataSource4() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .build();
    }

    @Bean(name = "jdbcTemplate4")
    public JdbcTemplate jdbcTemplate4(@Qualifier("dataSource4") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "namedJdbcTemplate4")
    public NamedParameterJdbcTemplate namedJdbcTemplate4(@Qualifier("dataSource4") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * tm4 - 业务事务管理器
     * 用于管理 db4 的业务数据操作
     */
    @Bean(name = "tm4")
    public PlatformTransactionManager tm4(@Qualifier("dataSource4") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
