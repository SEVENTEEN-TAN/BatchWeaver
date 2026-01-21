package com.batchweaver.core.config;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Batch 基础设施配置 - 元数据事务独立配置
 * <p>
 * 核心设计：JobRepository 绑定 tm1Meta（元数据专用事务管理器）
 * 确保元数据事务独立于业务事务，失败时元数据也能提交
 * <p>
 * 事务隔离保证：
 * - JobRepository 使用 tm1Meta 管理元数据表（BATCH_JOB_EXECUTION、BATCH_STEP_EXECUTION 等）
 * - Step 使用 tm1/tm2/tm3/tm4 管理业务数据（必须显式指定）
 * - 业务失败时：业务事务回滚，tm1Meta 提交 FAILED 状态
 * <p>
 * 注意：不使用 @EnableBatchProcessing，改为显式定义所有基础设施 Bean，
 * 避免 Bean 重复注册冲突，并确保数据源和事务管理器正确绑定。
 */
@Configuration
public class BatchInfrastructureConfig {

    /**
     * 关键配置：JobRepository 绑定 tm1Meta（元数据专用事务管理器）
     * 确保元数据事务独立于业务事务，失败时元数据也能提交
     *
     * @param dataSource1 db1 数据源（元数据表所在数据库）
     * @param tm1Meta     元数据专用事务管理器（标记 @Primary）
     * @return JobRepository
     */
    @Bean
    public JobRepository jobRepository(@Qualifier("dataSource1") DataSource dataSource1,
                                       @Qualifier("tm1Meta") PlatformTransactionManager tm1Meta) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource1);       // ✅ 使用 db1 数据源
        factory.setTransactionManager(tm1Meta);   // 绑定 tm1Meta，确保元数据事务独立
        factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
        factory.setTablePrefix("BATCH_");         // Spring Batch 元数据表前缀
        factory.setDatabaseType("SQLSERVER");     // SQL Server 数据库类型
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * JobLauncher 配置（使用上面的 JobRepository）
     */
    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    /**
     * JobExplorer 配置（用于查询批处理执行历史）
     */
    @Bean
    public JobExplorer jobExplorer(@Qualifier("dataSource1") DataSource dataSource1) throws Exception {
        JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
        factory.setDataSource(dataSource1);
        factory.setTablePrefix("BATCH_");
        // Spring Batch 5.x 会自动检测数据库类型，无需手动设置
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * JobRegistry 配置（用于注册和查找 Job）
     * JobOperator 需要通过 JobRegistry 来查找 Job
     */
    @Bean
    public JobRegistry jobRegistry() {
        return new org.springframework.batch.core.configuration.support.MapJobRegistry();
    }

    /**
     * JobRegistryBeanPostProcessor 配置
     * 自动将所有 Job Bean 注册到 JobRegistry
     */
    @Bean
    public JobRegistryBeanPostProcessor jobRegistryBeanPostProcessor(JobRegistry jobRegistry) {
        JobRegistryBeanPostProcessor postProcessor = new JobRegistryBeanPostProcessor();
        postProcessor.setJobRegistry(jobRegistry);
        return postProcessor;
    }

    /**
     * JobOperator 配置（支持 Job 重启等高级操作）
     * 用于断点续传场景
     */
    @Bean
    public JobOperator jobOperator(JobExplorer jobExplorer, JobRepository jobRepository,
                                    JobRegistry jobRegistry, JobLauncher jobLauncher) throws Exception {
        SimpleJobOperator jobOperator = new SimpleJobOperator();
        jobOperator.setJobExplorer(jobExplorer);
        jobOperator.setJobRepository(jobRepository);
        jobOperator.setJobRegistry(jobRegistry);
        jobOperator.setJobLauncher(jobLauncher);
        return jobOperator;
    }
}
