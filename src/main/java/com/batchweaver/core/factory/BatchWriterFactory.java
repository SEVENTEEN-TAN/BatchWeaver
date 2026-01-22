package com.batchweaver.core.factory;

import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Batch Writer 工厂
 * <p>
 * 统一创建 ItemWriter，并返回 StreamableWriter 记录类，
 * 强制调用方在 Step 中注册 stream，避免遗漏 .stream() 调用。
 * <p>
 * 使用示例：
 * <pre>{@code
 * var writerPair = writerFactory.createFlatFileWriter(
 *     "csvWriter",
 *     new FileSystemResource("output.csv"),
 *     builder -> builder.delimited().delimiter(",").names("id", "name")
 * );
 *
 * return new StepBuilder("exportStep", jobRepository)
 *         .chunk(100, tm2)
 *         .reader(reader)
 *         .writer(writerPair.writer())
 *         .stream(writerPair.stream())  // ✅ 强制注册 stream
 *         .build();
 * }</pre>
 *
 * @author BatchWeaver Team
 * @since 1.0.0
 */
@Component
public class BatchWriterFactory {

    /**
     * 创建 FlatFileItemWriter 并返回 StreamableWriter
     * <p>
     * 通过 Consumer 自定义 Writer 配置，返回的 StreamableWriter 包含
     * writer 和 stream 两个字段，强制调用方显式注册 stream。
     *
     * @param name       Writer 名称
     * @param resource   输出资源
     * @param customizer Writer 配置器
     * @param <T>        实体类型
     * @return StreamableWriter 记录类
     */
    public <T> StreamableWriter<T> createFlatFileWriter(
            String name,
            WritableResource resource,
            Consumer<FlatFileItemWriterBuilder<T>> customizer) {

        FlatFileItemWriterBuilder<T> builder = new FlatFileItemWriterBuilder<>();
        builder.name(name).resource(resource);
        customizer.accept(builder);

        FlatFileItemWriter<T> writer = builder.build();
        return new StreamableWriter<>(writer, writer);
    }

    /**
     * StreamableWriter 记录类
     * <p>
     * 包含 writer 和 stream 两个字段，强制调用方在 Step 中显式注册 stream。
     * 这样可以避免忘记调用 .stream(writer) 导致的 WriterNotOpenException。
     *
     * @param writer Writer 实例
     * @param stream Stream 实例（通常与 writer 相同）
     * @param <T>    实体类型
     */
    public record StreamableWriter<T>(
            FlatFileItemWriter<T> writer,
            ItemStream stream
    ) {
    }
}
