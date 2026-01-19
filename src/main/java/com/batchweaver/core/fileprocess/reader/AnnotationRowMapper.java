package com.batchweaver.core.fileprocess.reader;

import com.batchweaver.core.annotation.FileColumn;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 基于注解的RowMapper
 * <p>
 * 根据@FileColumn注解的name属性，从ResultSet映射到实体对象
 */
public class AnnotationRowMapper<T> implements RowMapper<T> {

    private final Class<T> targetType;

    public AnnotationRowMapper(Class<T> targetType) {
        this.targetType = targetType;
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {
            T instance = targetType.getDeclaredConstructor().newInstance();

            for (Field field : targetType.getDeclaredFields()) {
                if (field.isAnnotationPresent(FileColumn.class)) {
                    FileColumn annotation = field.getAnnotation(FileColumn.class);
                    String columnName = annotation.name().isEmpty() ? field.getName() : annotation.name();

                    Object value = rs.getObject(columnName);
                    if (value != null) {
                        field.setAccessible(true);
                        field.set(instance, value);
                    }
                }
            }

            return instance;
        } catch (Exception e) {
            throw new SQLException("Failed to map row to " + targetType.getSimpleName(), e);
        }
    }
}
