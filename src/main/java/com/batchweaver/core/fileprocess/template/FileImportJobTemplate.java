package com.batchweaver.core.fileprocess.template;

import com.batchweaver.core.fileprocess.function.*;
import com.batchweaver.core.fileprocess.listener.HeaderFooterListener;
import com.batchweaver.core.fileprocess.listener.UniversalErrorListener;
import lombok.Builder;
import lombok.Data;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 文件导入Job构建模板
 * <p>
 * 提供统一的Job构建方法，简化导入Job的创建
 */
public class FileImportJobTemplate {

    /**
     * 构建导入Job
     *
     * @param definition Job定义
     * @return Job实例
     */
    public Job buildJob(FileImportJobDefinition definition) {
        Step step = buildStep(definition);

        return new JobBuilder(definition.getJobName(), definition.getJobRepository())
            .start(step)
            .build();
    }

    /**
     * 构建导入Step
     *
     * @param definition Job定义
     * @return Step实例
     */
    @SuppressWarnings("unchecked")
    public Step buildStep(FileImportJobDefinition definition) {
        var chunkBuilder = new StepBuilder(definition.getStepName(), definition.getJobRepository())
            .<Object, Object>chunk(definition.getChunkSize(), definition.getTransactionManager())
            .reader(definition.getReader())
            .writer((org.springframework.batch.item.ItemWriter<? super Object>) definition.getWriter());

        // 可选：Processor
        if (definition.getProcessor() != null) {
            chunkBuilder.processor((org.springframework.batch.item.ItemProcessor<? super Object, ? extends Object>) definition.getProcessor());
        }

        // 可选：头尾校验
        if (definition.getHeaderParser() != null || definition.getFooterParser() != null) {
            chunkBuilder.listener(new HeaderFooterListener(
                definition.getResource(),
                definition.getHeaderParser(),
                definition.getHeaderValidator(),
                definition.getFooterParser(),
                definition.getFooterValidator()
            ));
        }

        // 错误处理
        if (definition.getSkipLimit() > 0) {
            chunkBuilder
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(definition.getSkipLimit())
                .listener(new UniversalErrorListener());
        }

        // 可选：Retry
        if (definition.getRetryLimit() > 0) {
            chunkBuilder
                .faultTolerant()
                .retry(Exception.class)
                .retryLimit(definition.getRetryLimit());
        }

        return chunkBuilder.build();
    }

    /**
     * 文件导入Job定义
     */
    @Data
    @Builder
    public static class FileImportJobDefinition {
        private String jobName;
        private String stepName;
        private JobRepository jobRepository;
        private PlatformTransactionManager transactionManager;

        // Reader/Processor/Writer
        private ItemReader<?> reader;
        private ItemProcessor<?, ?> processor;
        private ItemWriter<?> writer;

        // 头尾处理
        private Resource resource;
        private HeaderParser headerParser;
        private HeaderValidator headerValidator;
        private FooterParser footerParser;
        private FooterValidator footerValidator;

        // 性能配置
        @Builder.Default
        private int chunkSize = 1000;

        // 错误处理
        @Builder.Default
        private int skipLimit = 100;

        @Builder.Default
        private int retryLimit = 3;
    }
}
