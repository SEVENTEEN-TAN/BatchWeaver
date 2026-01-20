package com.batchweaver.core.fileprocess.writer;

import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;

import javax.sql.DataSource;

/**
 * JdbcBatchItemWriter工厂
 * <p>
 * 提供批量写入优化配置
 */
public class JdbcBatchItemWriterFactory {

    /**
     * 创建批量写入器
     *
     * @param dataSource 数据源
     * @param sql        SQL语句
     * @param itemType   实体类型
     * @param <T>        实体类型
     * @return JdbcBatchItemWriter
     */
    public static <T> JdbcBatchItemWriter<T> create(DataSource dataSource, String sql, Class<T> itemType) {
        return new JdbcBatchItemWriterBuilder<T>()
                .dataSource(dataSource)
                .sql(sql)
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .assertUpdates(false)  // 不强制校验更新数量（提升性能）
                .build();
    }

    /**
     * 创建批量写入器（带批量大小配置）
     *
     * @param dataSource 数据源
     * @param sql        SQL语句
     * @param itemType   实体类型
     * @param batchSize  批量大小（建议1000）
     * @param <T>        实体类型
     * @return JdbcBatchItemWriter
     */
    public static <T> JdbcBatchItemWriter<T> create(DataSource dataSource, String sql, Class<T> itemType, int batchSize) {
        JdbcBatchItemWriter<T> writer = create(dataSource, sql, itemType);
        // 注意：JdbcBatchItemWriter的批量大小由chunk size控制，这里只是示例
        return writer;
    }
}
