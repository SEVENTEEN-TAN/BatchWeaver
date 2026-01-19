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
 * DataSource3 配置 - db3（纯业务数据库）
 *
 * 业务事务管理器：tm3（用于 Step 的业务数据操作）
 */
@Configuration
public class DataSource3Config {

    @Bean(name = "dataSource3")
    @ConfigurationProperties(prefix = "spring.datasource.db3")
    public DataSource dataSource3() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .build();
    }

    @Bean(name = "jdbcTemplate3")
    public JdbcTemplate jdbcTemplate3(@Qualifier("dataSource3") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "namedJdbcTemplate3")
    public NamedParameterJdbcTemplate namedJdbcTemplate3(@Qualifier("dataSource3") DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * tm3 - 业务事务管理器
     * 用于管理 db3 的业务数据操作
     */
    @Bean(name = "tm3")
    public PlatformTransactionManager tm3(@Qualifier("dataSource3") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
