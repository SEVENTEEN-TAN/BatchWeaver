package com.batchweaver.demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

/**
 * 测试基类
 *
 * 提供统一的测试环境和通用方法
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false"
})
public abstract class AbstractBatchTest {

    @Autowired
    protected JobLauncher jobLauncher;

    @Autowired
    protected JobRepository jobRepository;

    @Autowired
    @Qualifier("dataSource2")
    protected DataSource businessDataSource;

    protected JdbcTemplate businessJdbcTemplate;

    @BeforeEach
    public void setUp() {
        this.businessJdbcTemplate = new JdbcTemplate(businessDataSource);
        // 清空业务表
        businessJdbcTemplate.execute("DELETE FROM DEMO_USER");
    }

    @AfterEach
    public void tearDown() {
        // 清理资源
    }

    /**
     * 启动 Job
     */
    protected JobExecution launchJob(Job job, JobParameters jobParameters) throws Exception {
        return jobLauncher.run(job, jobParameters);
    }

    /**
     * 创建基础 Job 参数
     */
    protected JobParameters createJobParameters() {
        return new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
    }

    /**
     * 验证业务表记录数
     */
    protected int getBusinessRecordCount() {
        return businessJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM DEMO_USER",
                Integer.class
        );
    }
}
