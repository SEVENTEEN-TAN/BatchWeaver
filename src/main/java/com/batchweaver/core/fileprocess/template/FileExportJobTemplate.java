package com.batchweaver.core.fileprocess.template;

import com.batchweaver.core.fileprocess.function.FooterGenerator;
import com.batchweaver.core.fileprocess.function.HeaderGenerator;
import com.batchweaver.core.fileprocess.listener.UniversalErrorListener;
import com.batchweaver.core.fileprocess.writer.AnnotationFieldExtractor;
import lombok.Builder;
import lombok.Data;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;

/**
 * 文件导出Job构建模板
 * <p>
 * 提供统一的导出Job构建方法
 */
public class FileExportJobTemplate {

    /**
     * 构建导出Job
     */
    public <T> Job buildJob(FileExportJobDefinition<T> definition) {
        Step step = buildStep(definition);

        return new JobBuilder(definition.getJobName(), definition.getJobRepository())
            .start(step)
            .build();
    }

    /**
     * 构建导出Step
     */
    public <T> Step buildStep(FileExportJobDefinition<T> definition) {
        // 构建Writer
        FlatFileItemWriter<T> writer = buildWriter(definition);

        var chunkBuilder = new StepBuilder(definition.getStepName(), definition.getJobRepository())
            .<T, T>chunk(definition.getChunkSize(), definition.getTransactionManager())
            .reader(definition.getReader())
            .writer(writer);

        // 错误处理
        if (definition.getSkipLimit() > 0) {
            chunkBuilder
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(definition.getSkipLimit())
                .listener(new UniversalErrorListener());
        }

        return chunkBuilder.build();
    }

    private <T> FlatFileItemWriter<T> buildWriter(FileExportJobDefinition<T> definition) {
        FlatFileItemWriterBuilder<T> builder = new FlatFileItemWriterBuilder<>();

        builder.name(definition.getStepName() + "Writer")
            .resource(definition.getResource())
            .lineAggregator(new DelimitedLineAggregator<T>() {{
                setDelimiter(definition.getDelimiter());
                setFieldExtractor(new AnnotationFieldExtractor<>(definition.getEntityClass()));
            }});

        // 头生成
        if (definition.getHeaderGenerator() != null) {
            builder.headerCallback(writer -> {
                String header = definition.getHeaderGenerator().generate(LocalDate.now());
                if (header != null && !header.isEmpty()) {
                    writer.write(header);
                }
            });
        }

        // 尾生成
        if (definition.getFooterGenerator() != null) {
            builder.footerCallback(new org.springframework.batch.item.file.FlatFileFooterCallback() {
                @Override
                public void writeFooter(Writer writer) throws IOException {
                    var context = StepSynchronizationManager.getContext();
                    if (context == null) {
                        throw new IllegalStateException("No step context available for footer generation");
                    }
                    StepExecution stepExecution = context.getStepExecution();
                    long writeCount = stepExecution.getWriteCount();
                    String footer = definition.getFooterGenerator().generate(writeCount);
                    if (footer != null && !footer.isEmpty()) {
                        writer.write(footer);
                    }
                }
            });
        }

        return builder.build();
    }

    /**
     * 文件导出Job定义
     */
    @Data
    @Builder
    public static class FileExportJobDefinition<T> {
        private String jobName;
        private String stepName;
        private JobRepository jobRepository;
        private PlatformTransactionManager transactionManager;

        private ItemReader<T> reader;
        private WritableResource resource;
        private Class<T> entityClass;

        @Builder.Default
        private String delimiter = ",";

        private HeaderGenerator headerGenerator;
        private FooterGenerator footerGenerator;

        @Builder.Default
        private int chunkSize = 1000;

        @Builder.Default
        private int skipLimit = 100;
    }
}
