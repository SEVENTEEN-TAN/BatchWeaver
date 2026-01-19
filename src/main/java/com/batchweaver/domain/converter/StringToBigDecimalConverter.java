package com.batchweaver.domain.converter;

import java.math.BigDecimal;

/**
 * String → BigDecimal 转换器
 */
public class StringToBigDecimalConverter implements TypeConverter<BigDecimal> {

    @Override
    public BigDecimal convert(String value) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return new BigDecimal(value.trim());
    }
}
