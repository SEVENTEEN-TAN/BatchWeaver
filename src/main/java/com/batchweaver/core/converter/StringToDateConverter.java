package com.batchweaver.core.converter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * String → Date 转换器
 * 默认日期格式：yyyyMMdd
 */
public class StringToDateConverter implements TypeConverter<Date> {

    private static final String DEFAULT_DATE_FORMAT = "yyyyMMdd";

    @Override
    public Date convert(String value) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
        return sdf.parse(value.trim());
    }
}
