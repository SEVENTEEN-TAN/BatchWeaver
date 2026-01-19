package com.batchweaver.domain.converter;

/**
 * 空操作转换器（默认转换器）
 */
public class NoOpConverter implements TypeConverter<String> {

    @Override
    public String convert(String value) {
        return value;
    }
}
