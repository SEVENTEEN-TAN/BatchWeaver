package com.batchweaver.core.factory;

import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Batch Reader 工厂
 * <p>
 * 统一创建和初始化 ItemReader，封装 afterPropertiesSet() 调用，
 * 避免手动创建时遗漏初始化步骤。
 * <p>
 * 使用示例：
 * <pre>{@code
 * JdbcPagingItemReader<DemoUser> reader = readerFactory.createJdbcPagingReader(
 *     "userReader",
 *     dataSource,
 *     queryProvider,
 *     new BeanPropertyRowMapper<>(DemoUser.class),
 *     100
 * );
 * }</pre>
 *
 * @author BatchWeaver Team
 * @since 1.0.0
 */
@Component
public class BatchReaderFactory {

    /**
     * 创建 JDBC 分页 Reader
     * <p>
     * 自动调用 afterPropertiesSet() 完成初始化，确保 Reader 可以立即使用。
     *
     * @param name          Reader 名称（用于日志和监控）
     * @param dataSource    数据源
     * @param queryProvider 分页查询提供器
     * @param itemType      实体类型
     * @param pageSize      分页大小（建议 100-1000）
     * @param <T>           实体类型
     * @return 已初始化的 JdbcPagingItemReader
     * @throws Exception 如果初始化失败
     */
    public <T> JdbcPagingItemReader<T> createJdbcPagingReader(
            String name,
            DataSource dataSource,
            PagingQueryProvider queryProvider,
            Class<T> itemType,
            int pageSize) throws Exception {

        JdbcPagingItemReader<T> reader = new JdbcPagingItemReader<>();
        reader.setName(name);
        reader.setDataSource(dataSource);
        reader.setQueryProvider(queryProvider);
        reader.setRowMapper(new BeanPropertyRowMapper<>(itemType));
        reader.setPageSize(pageSize);
        reader.afterPropertiesSet();  // ✅ 统一初始化，避免遗漏

        return reader;
    }
}
