package com.batchweaver.core.fileprocess.function;

import com.batchweaver.core.fileprocess.model.FooterInfo;

/**
 * 尾解析器函数接口
 * <p>
 * 用于解析文件尾行，提取尾信息（如记录数、校验和等）
 */
@FunctionalInterface
public interface FooterParser {

    /**
     * 解析尾行
     *
     * @param line 尾行内容
     * @return 尾信息
     * @throws Exception 解析失败时抛出异常
     */
    FooterInfo parse(String line) throws Exception;
}
