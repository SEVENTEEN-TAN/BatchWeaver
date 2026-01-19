package com.batchweaver.fileprocess.function;

import com.batchweaver.fileprocess.model.HeaderInfo;

/**
 * 头解析器函数接口
 * <p>
 * 用于解析文件头行，提取头信息（如日期、文件类型等）
 */
@FunctionalInterface
public interface HeaderParser {

    /**
     * 解析头行
     *
     * @param line 头行内容
     * @return 头信息
     * @throws Exception 解析失败时抛出异常
     */
    HeaderInfo parse(String line) throws Exception;
}
