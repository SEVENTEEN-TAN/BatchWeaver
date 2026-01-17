package com.example.batch.demo.job.config;

import com.example.batch.core.execution.BatchJob;
import com.example.batch.demo.infrastructure.entity.DemoUserEntity;
import com.example.batch.demo.infrastructure.mapper.DemoUserRowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Chunk 模式示例：使用 Spring Batch 原生 Reader/Writer
 *
 * 场景：从 DB1 读取 PENDING 用户 → 处理转换 → 写入 DB2
 *
 * 优势：
 * - 使用 JdbcCursorItemReader 流式读取（适合大数据量）
 * - 使用 JdbcBatchItemWriter 批量写入（高性能）
 * - Chunk 大小可配置（如 100 条一批）
 * - 事务由 Step 的 TransactionManager 管理
 * - 支持失败重试和跳过
 */
@Slf4j
@Configuration
@BatchJob(name = "chunkDemoJob", steps = {"chunkDemoStep"})
public class ChunkDemoJobConfig {

    @Bean
    public Job chunkDemoJob(JobRepository jobRepository, Step chunkDemoStep) {
        return new JobBuilder("chunkDemoJob", jobRepository)
                .start(chunkDemoStep)
                .build();
    }

    /**
     * Chunk Step：从 DB1 读取 → 处理 → 写入 DB2
     *
     * 配置：
     * - Reader: JdbcCursorItemReader（从 DB1 读取 PENDING 用户）
     * - Processor: 转换状态为 PROCESSING
     * - Writer: JdbcBatchItemWriter（批量写入 DB2）
     * - Chunk Size: 100（每 100 条提交一次事务）
     * - 事务管理器: tm2（写入目标数据库 DB2）
     */
    @Bean
    public Step chunkDemoStep(JobRepository jobRepository,
                          @Qualifier("tm2") PlatformTransactionManager tm2,
                          JdbcCursorItemReader<DemoUserEntity> reader,
                          ItemProcessor<DemoUserEntity, DemoUserEntity> processor,
                          JdbcBatchItemWriter<DemoUserEntity> writer) {
        return new StepBuilder("chunkDemoStep", jobRepository)
                .<DemoUserEntity, DemoUserEntity>chunk(100, tm2)  // 每 100 条一个 Chunk
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    /**
     * Reader：从 DB1 读取 PENDING 状态的用户
     *
     * 特点：
     * - 使用游标流式读取，内存占用小
     * - 适合大数据量场景（百万级）
     * - 支持自动分页
     */
    @Bean
    public JdbcCursorItemReader<DemoUserEntity> reader(@Qualifier("ds1") DataSource ds1) {
        String sql = "SELECT * FROM DEMO_USER WHERE STATUS = 'PENDING'";

        return new JdbcCursorItemReaderBuilder<DemoUserEntity>()
                .name("userReader")
                .dataSource(ds1)
                .sql(sql)
                .rowMapper(DemoUserRowMapper.INSTANCE)
                .build();
    }

    /**
     * Processor：处理业务逻辑
     *
     * 示例：将状态从 PENDING 转换为 PROCESSING
     * 可以添加复杂的业务逻辑、数据清洗、格式转换等
     */
    @Bean
    public ItemProcessor<DemoUserEntity, DemoUserEntity> processor() {
        return user -> {
            log.debug("Processing user: {}", user.getUsername());

            // 业务处理：转换状态
            user.setStatus("PROCESSING");

            // 示例：可以添加更多处理逻辑
            // - 数据清洗
            // - 格式转换
            // - 调用外部 API
            // - 过滤数据（返回 null 则跳过该条）

            return user;
        };
    }

    /**
     * Writer：批量写入 DB2
     *
     * 特点：
     * - 使用 PreparedStatement 批量写入
     * - 性能优异（相比逐条插入提升 10-100 倍）
     * - 支持命名参数
     * - 事务由 tm2 管理
     */
    @Bean
    public JdbcBatchItemWriter<DemoUserEntity> writer(@Qualifier("ds2") DataSource ds2) {
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (:username, :email, :status, GETDATE())";

        return new JdbcBatchItemWriterBuilder<DemoUserEntity>()
                .dataSource(ds2)
                .sql(sql)
                .beanMapped()  // 自动将 Entity 属性映射为命名参数
                .build();
    }

    /**
     * 高级用法示例：自定义 ItemWriter（更灵活）
     *
     * 如果需要更复杂的写入逻辑，可以手动实现 ItemPreparedStatementSetter
     */
    /*
    @Bean
    public JdbcBatchItemWriter<DemoUserEntity> customWriter(@Qualifier("ds2") DataSource ds2) {
        String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS, UPDATE_TIME) " +
                     "VALUES (?, ?, ?, GETDATE())";

        JdbcBatchItemWriter<DemoUserEntity> writer = new JdbcBatchItemWriter<>();
        writer.setDataSource(ds2);
        writer.setSql(sql);
        writer.setItemPreparedStatementSetter((item, ps) -> {
            ps.setString(1, item.getUsername());
            ps.setString(2, item.getEmail());
            ps.setString(3, item.getStatus());
        });

        return writer;
    }
    */
}
