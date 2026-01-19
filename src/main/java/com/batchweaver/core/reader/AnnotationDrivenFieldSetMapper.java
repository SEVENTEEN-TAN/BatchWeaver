package com.batchweaver.core.reader;

import com.batchweaver.domain.annotation.FileColumn;
import com.batchweaver.domain.converter.NoOpConverter;
import com.batchweaver.domain.converter.TypeConverter;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 基于注解的字段映射器
 * <p>
 * 解析实体类上的 @FileColumn 注解，自动完成字段映射、数据清洗和类型转换
 *
 * @param <T> 目标实体类型
 */
public class AnnotationDrivenFieldSetMapper<T> implements FieldSetMapper<T> {

    private final Class<T> targetType;

    public AnnotationDrivenFieldSetMapper(Class<T> targetType) {
        this.targetType = targetType;
    }

    @Override
    public T mapFieldSet(FieldSet fieldSet) throws BindException {
        try {
            T instance = targetType.getDeclaredConstructor().newInstance();

            for (Field field : targetType.getDeclaredFields()) {
                if (field.isAnnotationPresent(FileColumn.class)) {
                    FileColumn annotation = field.getAnnotation(FileColumn.class);
                    String value = fieldSet.readString(annotation.index());

                    // 数据清洗
                    value = cleanValue(value, annotation);

                    // 默认值填充
                    if ((value == null || value.isEmpty()) && !annotation.defaultValue().isEmpty()) {
                        value = annotation.defaultValue();
                    }

                    // 类型转换
                    Object convertedValue = convertValue(value, field.getType(), annotation.converter());

                    // 设置字段值
                    field.setAccessible(true);
                    field.set(instance, convertedValue);
                }
            }

            return instance;
        } catch (Exception e) {
            throw new BindException(null, "Failed to map FieldSet to " + targetType.getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * 数据清洗
     */
    private String cleanValue(String value, FileColumn annotation) {
        if (value == null) {
            return null;
        }

        if (annotation.trim()) {
            value = value.trim();
        }

        if (annotation.toUpperCase()) {
            value = value.toUpperCase();
        }

        if (annotation.toLowerCase()) {
            value = value.toLowerCase();
        }

        return value;
    }

    /**
     * 类型转换
     */
    private Object convertValue(String value, Class<?> targetType, Class<? extends TypeConverter<?>> converterClass) throws Exception {
        if (value == null || value.isEmpty()) {
            return null;
        }

        // 使用自定义转换器
        if (converterClass != NoOpConverter.class) {
            TypeConverter<?> converter = converterClass.getDeclaredConstructor().newInstance();
            return converter.convert(value);
        }

        // 内置转换器
        if (targetType == String.class) {
            return value;
        } else if (targetType == Integer.class || targetType == int.class) {
            return Integer.valueOf(value);
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.valueOf(value);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.valueOf(value);
        } else if (targetType == BigDecimal.class) {
            return new BigDecimal(value);
        } else if (targetType == Date.class) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            return sdf.parse(value);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.valueOf(value);
        }

        return value;
    }
}
