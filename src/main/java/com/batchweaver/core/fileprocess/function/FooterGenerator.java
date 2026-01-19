package com.batchweaver.core.fileprocess.function;

/**
 * 尾生成器函数接口
 * <p>
 * 用于生成文件尾行（导出场景）
 */
@FunctionalInterface
public interface FooterGenerator {

    /**
     * 生成尾行
     *
     * @param recordCount 记录数
     * @return 尾行内容
     */
    String generate(long recordCount);
}
