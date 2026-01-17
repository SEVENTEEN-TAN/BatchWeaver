package com.example.batch.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 多数据源与事务管理器配置（JdbcTemplate 方案）
 *
 * 核心设计思想：
 * 1. 为每个数据库创建独立的 DataSource、TransactionManager 和 JdbcTemplate
 * 2. 每个 Step 直接注入对应的 JdbcTemplate（如 jdbcTemplate2）
 * 3. Service 方法通过构造函数注入特定的 JdbcTemplate，无需动态路由
 * 4. 事务由 Step 的 TransactionManager 完全控制
 *
 * 优势：
 * - 事务管理极其简单：每个 Step 的事务边界明确
 * - 无需 MyBatis-Flex 的 @UseDataSource 路由
 * - 无需 TransactionAwareDataSourceProxy
 * - 避免多数据源非原子性问题
 * - 与 Spring Batch 原生推荐方案一致
 */
@Slf4j
@Configuration
public class MultiDataSourceConfig {

    // ==================== DB1 (元数据 + Step1 业务) ====================

    @Value("${spring.datasource.db1.url}")
    private String db1Url;

    @Value("${spring.datasource.db1.username}")
    private String db1Username;

    @Value("${spring.datasource.db1.password}")
    private String db1Password;

    @Value("${spring.datasource.db1.driver-class-name}")
    private String db1DriverClassName;

    @Bean("ds1")
    @Primary
    public DataSource ds1() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(db1Url);
        config.setUsername(db1Username);
        config.setPassword(db1Password);
        config.setDriverClassName(db1DriverClassName);
        config.setPoolName("HikariPool-DB1");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionTestQuery("SELECT 1");

        log.info("初始化数据源 ds1 (DB1): {}", db1Url);
        return new HikariDataSource(config);
    }

    @Bean("tm1")
    @Primary
    public PlatformTransactionManager tm1(@Qualifier("ds1") DataSource ds1) {
        log.info("创建事务管理器 tm1 for DB1 (元数据 + Step1 业务)");
        return new DataSourceTransactionManager(ds1);
    }

    @Bean("jdbcTemplate1")
    @Primary
    public JdbcTemplate jdbcTemplate1(@Qualifier("ds1") DataSource ds1) {
        log.info("创建 JdbcTemplate1 for DB1");
        return new JdbcTemplate(ds1);
    }

    @Bean("namedJdbcTemplate1")
    @Primary
    public NamedParameterJdbcTemplate namedJdbcTemplate1(@Qualifier("ds1") DataSource ds1) {
        return new NamedParameterJdbcTemplate(ds1);
    }

    // ==================== DB2 (Step2 业务) ====================

    @Value("${spring.datasource.db2.url}")
    private String db2Url;

    @Value("${spring.datasource.db2.username}")
    private String db2Username;

    @Value("${spring.datasource.db2.password}")
    private String db2Password;

    @Value("${spring.datasource.db2.driver-class-name}")
    private String db2DriverClassName;

    @Bean("ds2")
    public DataSource ds2() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(db2Url);
        config.setUsername(db2Username);
        config.setPassword(db2Password);
        config.setDriverClassName(db2DriverClassName);
        config.setPoolName("HikariPool-DB2");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionTestQuery("SELECT 1");

        log.info("初始化数据源 ds2 (DB2): {}", db2Url);
        return new HikariDataSource(config);
    }

    @Bean("tm2")
    public PlatformTransactionManager tm2(@Qualifier("ds2") DataSource ds2) {
        log.info("创建事务管理器 tm2 for DB2 (Step2 业务)");
        return new DataSourceTransactionManager(ds2);
    }

    @Bean("jdbcTemplate2")
    public JdbcTemplate jdbcTemplate2(@Qualifier("ds2") DataSource ds2) {
        log.info("创建 JdbcTemplate2 for DB2");
        return new JdbcTemplate(ds2);
    }

    @Bean("namedJdbcTemplate2")
    public NamedParameterJdbcTemplate namedJdbcTemplate2(@Qualifier("ds2") DataSource ds2) {
        return new NamedParameterJdbcTemplate(ds2);
    }

    // ==================== DB3 (Step3 业务) ====================

    @Value("${spring.datasource.db3.url}")
    private String db3Url;

    @Value("${spring.datasource.db3.username}")
    private String db3Username;

    @Value("${spring.datasource.db3.password}")
    private String db3Password;

    @Value("${spring.datasource.db3.driver-class-name}")
    private String db3DriverClassName;

    @Bean("ds3")
    public DataSource ds3() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(db3Url);
        config.setUsername(db3Username);
        config.setPassword(db3Password);
        config.setDriverClassName(db3DriverClassName);
        config.setPoolName("HikariPool-DB3");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionTestQuery("SELECT 1");

        log.info("初始化数据源 ds3 (DB3): {}", db3Url);
        return new HikariDataSource(config);
    }

    @Bean("tm3")
    public PlatformTransactionManager tm3(@Qualifier("ds3") DataSource ds3) {
        log.info("创建事务管理器 tm3 for DB3 (Step3 业务)");
        return new DataSourceTransactionManager(ds3);
    }

    @Bean("jdbcTemplate3")
    public JdbcTemplate jdbcTemplate3(@Qualifier("ds3") DataSource ds3) {
        log.info("创建 JdbcTemplate3 for DB3");
        return new JdbcTemplate(ds3);
    }

    @Bean("namedJdbcTemplate3")
    public NamedParameterJdbcTemplate namedJdbcTemplate3(@Qualifier("ds3") DataSource ds3) {
        return new NamedParameterJdbcTemplate(ds3);
    }

    // ==================== DB4 (Step4 业务) ====================

    @Value("${spring.datasource.db4.url}")
    private String db4Url;

    @Value("${spring.datasource.db4.username}")
    private String db4Username;

    @Value("${spring.datasource.db4.password}")
    private String db4Password;

    @Value("${spring.datasource.db4.driver-class-name}")
    private String db4DriverClassName;

    @Bean("ds4")
    public DataSource ds4() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(db4Url);
        config.setUsername(db4Username);
        config.setPassword(db4Password);
        config.setDriverClassName(db4DriverClassName);
        config.setPoolName("HikariPool-DB4");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setConnectionTestQuery("SELECT 1");

        log.info("初始化数据源 ds4 (DB4): {}", db4Url);
        return new HikariDataSource(config);
    }

    @Bean("tm4")
    public PlatformTransactionManager tm4(@Qualifier("ds4") DataSource ds4) {
        log.info("创建事务管理器 tm4 for DB4 (Step4 业务)");
        return new DataSourceTransactionManager(ds4);
    }

    @Bean("jdbcTemplate4")
    public JdbcTemplate jdbcTemplate4(@Qualifier("ds4") DataSource ds4) {
        log.info("创建 JdbcTemplate4 for DB4");
        return new JdbcTemplate(ds4);
    }

    @Bean("namedJdbcTemplate4")
    public NamedParameterJdbcTemplate namedJdbcTemplate4(@Qualifier("ds4") DataSource ds4) {
        return new NamedParameterJdbcTemplate(ds4);
    }
}
