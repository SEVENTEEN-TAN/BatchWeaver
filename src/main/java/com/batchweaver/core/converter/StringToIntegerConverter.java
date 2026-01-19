package com.batchweaver.core.converter;

/**
 * String → Integer 转换器
 */
public class StringToIntegerConverter implements TypeConverter<Integer> {

    @Override
    public Integer convert(String value) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Integer.valueOf(value.trim());
    }
}
