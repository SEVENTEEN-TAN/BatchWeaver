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
 * 关键数据源：承载 Spring Batch 元数据表 + 业务数据
 * - 元数据事务管理器：tm1Meta（绑定 JobRepository，标记 @Primary 作为默认事务管理器）
 * - 业务事务管理器：tm1（用于 db1 业务表操作，必须显式指定）
 * - 其他业务库：tm2/tm3/tm4（db2/db3/db4 业务操作，必须显式指定）
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
     * tm1Meta - 元数据事务管理器（@Primary）
     * <p>
     * 用于管理 Spring Batch 元数据表的事务，确保元数据事务独立于业务事务
     * <p>
     * 关键设计：
     * - JobRepository 必须绑定此事务管理器，确保即使业务失败，元数据也能提交
     * - 标记 @Primary：当业务代码忘记指定事务管理器时，默认使用此管理器（相对安全，不会把数据写错库）
     * - 业务代码应显式指定 tm1/tm2/tm3/tm4，而非依赖默认值
     */
    @Primary
    @Bean(name = "tm1Meta")
    public PlatformTransactionManager tm1Meta(@Qualifier("dataSource1") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * tm1 - db1 业务事务管理器
     * <p>
     * 用于管理 db1 业务表的事务操作
     * <p>
     * 注意：
     * - 此 Bean 未标记 @Primary，必须显式使用 @Qualifier("tm1") 指定
     * - 与 tm2/tm3/tm4 命名风格一致，都是业务事务管理器
     * - 与 tm1Meta 职责分离：tm1Meta 管元数据，tm1 管业务数据
     */
    @Bean(name = "tm1")
    public PlatformTransactionManager tm1(@Qualifier("dataSource1") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
