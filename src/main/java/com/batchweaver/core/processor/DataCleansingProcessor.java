package com.batchweaver.core.processor;

import org.springframework.batch.item.ItemProcessor;

/**
 * 数据清洗处理器（通用）
 *
 * 注：大部分数据清洗已在 AnnotationDrivenFieldSetMapper 中完成
 * 此 Processor 可用于额外的业务逻辑处理
 *
 * @param <T> 输入输出类型
 */
public class DataCleansingProcessor<T> implements ItemProcessor<T, T> {

    @Override
    public T process(T item) throws Exception {
        // 数据清洗逻辑已在 AnnotationDrivenFieldSetMapper 中完成
        // 这里可以添加额外的业务逻辑处理

        return item;  // 直接返回
    }
}
