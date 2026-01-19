package com.batchweaver.fileprocess.function;

import java.time.LocalDate;

/**
 * 头生成器函数接口
 * <p>
 * 用于生成文件头行（导出场景）
 */
@FunctionalInterface
public interface HeaderGenerator {

    /**
     * 生成头行
     *
     * @param date 日期
     * @return 头行内容
     */
    String generate(LocalDate date);
}
