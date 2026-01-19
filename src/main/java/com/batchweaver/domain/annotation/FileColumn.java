package com.batchweaver.domain.annotation;

import com.batchweaver.domain.converter.NoOpConverter;
import com.batchweaver.domain.converter.TypeConverter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 文件列映射注解
 * <p>
 * 用于标记实体类字段与文件列的映射关系，支持：
 * - 列索引指定
 * - 数据清洗（trim、大小写转换、默认值）
 * - 自定义类型转换器
 * <p>
 * 示例：
 * <pre>
 * {@code @FileColumn(index = 0, name = "userId")}
 * private Integer id;
 *
 * {@code @FileColumn(index = 1, trim = true, toUpperCase = true)}
 * private String name;
 *
 * {@code @FileColumn(index = 2, defaultValue = "unknown@example.com")}
 * private String email;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FileColumn {

    /**
     * 列索引（从 0 开始）
     */
    int index();

    /**
     * 列名称（用于日志和错误提示）
     */
    String name() default "";

    /**
     * 是否去除前后空格（默认 true）
     */
    boolean trim() default true;

    /**
     * 是否转大写
     */
    boolean toUpperCase() default false;

    /**
     * 是否转小写
     */
    boolean toLowerCase() default false;

    /**
     * 默认值（当字段为空时使用）
     */
    String defaultValue() default "";

    /**
     * 自定义类型转换器
     */
    Class<? extends TypeConverter<?>> converter() default NoOpConverter.class;
}
