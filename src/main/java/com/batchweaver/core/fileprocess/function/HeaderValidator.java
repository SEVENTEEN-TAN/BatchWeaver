package com.batchweaver.core.fileprocess.function;

import com.batchweaver.core.fileprocess.model.HeaderInfo;

/**
 * 头校验器函数接口
 * <p>
 * 用于校验头信息的有效性（如日期是否为今天、文件类型是否匹配等）
 */
@FunctionalInterface
public interface HeaderValidator {

    /**
     * 校验头信息
     *
     * @param header 头信息
     * @throws Exception 校验失败时抛出异常
     */
    void validate(HeaderInfo header) throws Exception;
}
