package com.batchweaver.fileprocess.function;

import com.batchweaver.fileprocess.model.FooterInfo;

/**
 * 尾校验器函数接口
 * <p>
 * 用于校验尾信息的有效性（如记录数是否匹配实际处理数量）
 */
@FunctionalInterface
public interface FooterValidator {

    /**
     * 校验尾信息
     *
     * @param footer      尾信息
     * @param actualCount 实际处理的记录数
     * @throws Exception 校验失败时抛出异常
     */
    void validate(FooterInfo footer, long actualCount) throws Exception;
}
