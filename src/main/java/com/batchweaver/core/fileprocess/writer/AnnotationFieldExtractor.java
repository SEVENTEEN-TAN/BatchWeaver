package com.batchweaver.core.fileprocess.writer;

import com.batchweaver.core.annotation.FileColumn;
import org.springframework.batch.item.file.transform.FieldExtractor;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * 基于注解的字段提取器
 * <p>
 * 根据@FileColumn注解的order属性，按顺序提取字段值
 */
public class AnnotationFieldExtractor<T> implements FieldExtractor<T> {

    private final Class<T> targetType;
    private final Field[] sortedFields;

    public AnnotationFieldExtractor(Class<T> targetType) {
        this.targetType = targetType;
        this.sortedFields = Arrays.stream(targetType.getDeclaredFields())
            .filter(f -> f.isAnnotationPresent(FileColumn.class))
            .sorted(Comparator.comparingInt(f -> f.getAnnotation(FileColumn.class).index()))
            .toArray(Field[]::new);
    }

    @Override
    public Object[] extract(T item) {
        Object[] values = new Object[sortedFields.length];

        for (int i = 0; i < sortedFields.length; i++) {
            Field field = sortedFields[i];
            field.setAccessible(true);

            try {
                Object value = field.get(item);
                values[i] = formatValue(value, field);
            } catch (IllegalAccessException e) {
                values[i] = null;
            }
        }

        return values;
    }

    private String formatValue(Object value, Field field) {
        if (value == null) {
            return "";
        }

        FileColumn annotation = field.getAnnotation(FileColumn.class);

        // 日期格式化
        if (value instanceof Date && !annotation.format().isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat(annotation.format());
            return sdf.format((Date) value);
        }

        return value.toString();
    }
}
