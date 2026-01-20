package com.batchweaver.core.fileprocess.template;

import com.batchweaver.core.fileprocess.function.FooterParser;
import com.batchweaver.core.fileprocess.function.FooterValidator;
import com.batchweaver.core.fileprocess.function.HeaderParser;
import com.batchweaver.core.fileprocess.function.HeaderValidator;
import com.batchweaver.core.fileprocess.listener.UniversalErrorListener;
import com.batchweaver.core.fileprocess.reader.FooterLineDetector;
import com.batchweaver.core.fileprocess.reader.HeaderFooterAwareReader;
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
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.validation.BindException;

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
    public <I, O> Job buildJob(FileImportJobDefinition<I, O> definition) {
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
    public <I, O> Step buildStep(FileImportJobDefinition<I, O> definition) {
        var chunkBuilder = new StepBuilder(definition.getStepName(), definition.getJobRepository())
                .<I, O>chunk(definition.getChunkSize(), definition.getTransactionManager())
                .reader(definition.getReader())
                .writer(definition.getWriter());

        // 可选：Processor
        if (definition.getProcessor() != null) {
            chunkBuilder.processor(definition.getProcessor());
        }

        // 注意：Header 和 Footer 校验已移至 HeaderFooterAwareReader 内部
        // 不再需要 HeaderFooterListener 和 FooterFilterListener

        // 错误处理：限定为可恢复的异常类型
        if (definition.getSkipLimit() > 0) {
            chunkBuilder
                    .faultTolerant()
                    .skip(FlatFileParseException.class)      // 文件解析异常
                    .skip(BindException.class)               // 字段绑定异常
                    .skip(IllegalArgumentException.class)     // 非法参数异常
                    .skipLimit(definition.getSkipLimit())
                    .listener(new UniversalErrorListener());
        }

        // 可选：Retry（同样限定异常类型）
        if (definition.getRetryLimit() > 0) {
            chunkBuilder
                    .faultTolerant()
                    .retry(FlatFileParseException.class)
                    .retry(BindException.class)
                    .retryLimit(definition.getRetryLimit());
        }

        return chunkBuilder.build();
    }

    /**
     * 延迟决策Reader - 基于"单次顺序扫描 + 延迟行确认"模式
     * <p>
     * <b>核心优势：</b>
     * <ul>
     *   <li>零启动延迟 - 不需要在open()时预先扫描整个文件</li>
     *   <li>O(1)内存占用 - 只缓存两行数据（prevLine + currentLine）</li>
     *   <li>适合超大文件（GB级）处理</li>
     *   <li>Reader 自包含头尾校验 - 校验失败直接抛异常</li>
     * </ul>
     * <p>
     * <b>使用示例：</b>
     * <pre>{@code
     * // 创建延迟决策Reader
     * HeaderFooterAwareReader<User> reader = template.createDeferredDecisionReader(
     *     resource,           // 文件资源
     *     headerParser,       // Header解析器（可选）
     *     headerValidator,    // Header校验器（可选）
     *     footerParser,       // Footer解析器（可选）
     *     footerValidator,    // Footer校验器（可选）
     *     lineTokenizer,      // 行分词器
     *     fieldSetMapper,     // 字段映射器
     *     footerLineDetector  // Footer检测器（可选）
     * );
     * }</pre>
     *
     * @param resource           文件资源
     * @param headerParser       Header解析器（可选）
     * @param headerValidator    Header校验器（可选）
     * @param footerParser       Footer解析器（可选）
     * @param footerValidator    Footer校验器（可选）
     * @param lineTokenizer      行分词器
     * @param fieldSetMapper     字段映射器
     * @param footerLineDetector Footer检测器（可选，默认支持纯数字、T|前缀、R前缀）
     * @param <T>                item type
     * @return 延迟决策Reader
     */
    public <T> HeaderFooterAwareReader<T> createDeferredDecisionReader(
            Resource resource,
            HeaderParser headerParser,
            HeaderValidator headerValidator,
            FooterParser footerParser,
            FooterValidator footerValidator,
            LineTokenizer lineTokenizer,
            FieldSetMapper<T> fieldSetMapper,
            FooterLineDetector footerLineDetector
    ) {
        return new HeaderFooterAwareReader<>(
                resource, headerParser, headerValidator, footerParser, footerValidator,
                lineTokenizer, fieldSetMapper, footerLineDetector
        );
    }

    /**
     * 延迟决策Reader - 使用默认Footer检测器
     *
     * @param resource        文件资源
     * @param headerParser    Header解析器（可选）
     * @param headerValidator Header校验器（可选）
     * @param footerParser    Footer解析器（可选）
     * @param footerValidator Footer校验器（可选）
     * @param lineTokenizer   行分词器
     * @param fieldSetMapper  字段映射器
     * @param <T>             item type
     * @return 延迟决策Reader
     */
    public <T> HeaderFooterAwareReader<T> createDeferredDecisionReader(
            Resource resource,
            HeaderParser headerParser,
            HeaderValidator headerValidator,
            FooterParser footerParser,
            FooterValidator footerValidator,
            LineTokenizer lineTokenizer,
            FieldSetMapper<T> fieldSetMapper
    ) {
        return new HeaderFooterAwareReader<>(
                resource, headerParser, headerValidator, footerParser, footerValidator,
                lineTokenizer, fieldSetMapper, FooterLineDetector.defaultDetector()
        );
    }

    /**
     * 延迟决策Reader - 向后兼容版本（不包含 FooterValidator）
     * <p>
     * <b>已废弃</b>：请使用包含 FooterValidator 参数的新方法
     *
     * @param resource           文件资源
     * @param headerParser       Header解析器（可选）
     * @param headerValidator    Header校验器（可选）
     * @param footerParser       Footer解析器（可选）
     * @param lineTokenizer      行分词器
     * @param fieldSetMapper     字段映射器
     * @param footerLineDetector Footer检测器（可选）
     * @param <T>                item type
     * @return 延迟决策Reader
     * @deprecated 请使用包含 FooterValidator 参数的新方法
     */
    @Deprecated
    public <T> HeaderFooterAwareReader<T> createDeferredDecisionReaderCompat(
            Resource resource,
            HeaderParser headerParser,
            HeaderValidator headerValidator,
            FooterParser footerParser,
            LineTokenizer lineTokenizer,
            FieldSetMapper<T> fieldSetMapper,
            FooterLineDetector footerLineDetector
    ) {
        return new HeaderFooterAwareReader<>(
                resource, headerParser, headerValidator, footerParser, null,
                lineTokenizer, fieldSetMapper, footerLineDetector
        );
    }

    /**
     * 延迟决策Reader - 向后兼容版本（不包含 FooterValidator 和 FooterLineDetector）
     * <p>
     * <b>已废弃</b>：请使用包含 FooterValidator 参数的新方法
     *
     * @param resource        文件资源
     * @param headerParser    Header解析器（可选）
     * @param headerValidator Header校验器（可选）
     * @param footerParser    Footer解析器（可选）
     * @param lineTokenizer   行分词器
     * @param fieldSetMapper  字段映射器
     * @param <T>             item type
     * @return 延迟决策Reader
     * @deprecated 请使用包含 FooterValidator 参数的新方法
     */
    @Deprecated
    public <T> HeaderFooterAwareReader<T> createDeferredDecisionReaderCompat(
            Resource resource,
            HeaderParser headerParser,
            HeaderValidator headerValidator,
            FooterParser footerParser,
            LineTokenizer lineTokenizer,
            FieldSetMapper<T> fieldSetMapper
    ) {
        return new HeaderFooterAwareReader<>(
                resource, headerParser, headerValidator, footerParser, null,
                lineTokenizer, fieldSetMapper, FooterLineDetector.defaultDetector()
        );
    }

    /**
     * 文件导入Job定义
     */
    @Data
    @Builder
    public static class FileImportJobDefinition<I, O> {
        private String jobName;
        private String stepName;
        private JobRepository jobRepository;
        private PlatformTransactionManager transactionManager;

        // Reader/Processor/Writer
        private ItemReader<? extends I> reader;
        private ItemProcessor<? super I, ? extends O> processor;
        private ItemWriter<? super O> writer;

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
