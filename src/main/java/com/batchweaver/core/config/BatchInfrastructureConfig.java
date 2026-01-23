package com.batchweaver.core.config;

import com.batchweaver.core.id.TimestampIncrementer;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchInfrastructureConfig {

    // Spring Batch 默认会用这三个“incrementerName”去取号
    private static final String JOB_SEQ = "BATCH_JOB_SEQ";
    private static final String JOB_EXECUTION_SEQ = "BATCH_JOB_EXECUTION_SEQ";
    private static final String STEP_EXECUTION_SEQ = "BATCH_STEP_EXECUTION_SEQ";

    @Bean
    public JobRepository jobRepository(
            @Qualifier("dataSource1") DataSource dataSource1,
            @Qualifier("tm1Meta") PlatformTransactionManager tm1Meta
    ) throws Exception {

        // 1) 时间戳 ID 生成器
        DataFieldMaxValueIncrementer timestampInc = new TimestampIncrementer();

        // 2) 默认工厂（保留 Spring Batch 对 databaseType 的支持校验 + 兜底实现）
        DefaultDataFieldMaxValueIncrementerFactory delegate =
                new DefaultDataFieldMaxValueIncrementerFactory(dataSource1);

        // 3) 自定义工厂：只替换 3 个 Batch 元数据主键的取号逻辑，其他走默认
        DataFieldMaxValueIncrementerFactory incrementerFactory = new DataFieldMaxValueIncrementerFactory() {
            @Override
            public DataFieldMaxValueIncrementer getIncrementer(String databaseType, String incrementerName) {
                if (incrementerName == null) {
                    return delegate.getIncrementer(databaseType, null);
                }
                String name = incrementerName.trim();
                if (JOB_SEQ.equalsIgnoreCase(name)
                        || JOB_EXECUTION_SEQ.equalsIgnoreCase(name)
                        || STEP_EXECUTION_SEQ.equalsIgnoreCase(name)) {
                    return timestampInc;
                }
                return delegate.getIncrementer(databaseType, incrementerName);
            }

            @Override
            public String[] getSupportedIncrementerTypes() {
                return delegate.getSupportedIncrementerTypes();
            }

            @Override
            public boolean isSupportedIncrementerType(String databaseType) {
                return delegate.isSupportedIncrementerType(databaseType);
            }
        };

        // 4) JobRepositoryFactoryBean：注意顺序 —— 全部 set 完，再 afterPropertiesSet() 一次
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource1);
        factory.setTransactionManager(tm1Meta);
        factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
        factory.setTablePrefix("BATCH_");
        factory.setDatabaseType("SQLSERVER");

        // ✅ 关键：一定要在 afterPropertiesSet() 之前设置
        factory.setIncrementerFactory(incrementerFactory);

        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    @Bean
    public JobExplorer jobExplorer(
            @Qualifier("dataSource1") DataSource dataSource1,
            @Qualifier("tm1Meta") PlatformTransactionManager tm1Meta
    ) throws Exception {
        JobExplorerFactoryBean factory = new JobExplorerFactoryBean();
        factory.setDataSource(dataSource1);
        factory.setTransactionManager(tm1Meta);
        factory.setTablePrefix("BATCH_");
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * JobRegistry 配置（用于注册和查找 Job）
     * 注意：Spring Batch 5.2+ 推荐直接从 ApplicationContext 获取 Job
     */
    @Bean
    public JobRegistry jobRegistry() {
        return new MapJobRegistry();
    }

    /**
     * JobOperator 配置（支持 Job 重启等高级操作）
     * 用于断点续传场景
     */
    @Bean
    public JobOperator jobOperator(
            JobExplorer jobExplorer,
            JobRepository jobRepository,
            JobRegistry jobRegistry,
            JobLauncher jobLauncher
    ) {
        SimpleJobOperator jobOperator = new SimpleJobOperator();
        jobOperator.setJobExplorer(jobExplorer);
        jobOperator.setJobRepository(jobRepository);
        jobOperator.setJobRegistry(jobRegistry);
        jobOperator.setJobLauncher(jobLauncher);
        return jobOperator;
    }
}
