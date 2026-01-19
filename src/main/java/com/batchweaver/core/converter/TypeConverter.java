package com.batchweaver.core.converter;

/**
 * 类型转换器接口
 * <p>
 * 用于将字符串值转换为目标类型
 *
 * @param <T> 目标类型
 */
public interface TypeConverter<T> {

    /**
     * 将字符串值转换为目标类型
     *
     * @param value 字符串值
     * @return 转换后的值
     * @throws Exception 转换失败时抛出异常
     */
    T convert(String value) throws Exception;
}
