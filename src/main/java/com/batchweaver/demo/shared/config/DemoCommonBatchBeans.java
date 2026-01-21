package com.batchweaver.demo.shared.config;

import com.batchweaver.core.fileprocess.function.FooterParser;
import com.batchweaver.core.fileprocess.function.FooterValidator;
import com.batchweaver.core.fileprocess.function.HeaderParser;
import com.batchweaver.core.fileprocess.function.HeaderValidator;
import com.batchweaver.core.fileprocess.function.PostImportValidator;
import com.batchweaver.core.fileprocess.listener.FooterValidationListener;
import com.batchweaver.core.fileprocess.model.FooterInfo;
import com.batchweaver.core.fileprocess.model.HeaderInfo;
import com.batchweaver.core.fileprocess.reader.FooterLineDetector;
import com.batchweaver.core.fileprocess.reader.HeaderFooterAwareReader;
import com.batchweaver.core.reader.AnnotationDrivenFieldSetMapper;
import com.batchweaver.demo.shared.entity.DemoUser;
import com.batchweaver.demo.shared.service.Db2BusinessService;
import com.batchweaver.demo.shared.service.Db3BusinessService;
import com.batchweaver.demo.shared.service.Db4BusinessService;
import com.batchweaver.demo.shared.entity.DemoUserInput;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

@Configuration
public class DemoCommonBatchBeans {


    /**
     * Reader：读取 large_users.txt
     * <p>
     * 文件格式：yyyyMMdd + 数据行 + count
     * 使用 HeaderFooterAwareReader 进行头尾校验
     */
    @Bean
    public HeaderFooterAwareReader<DemoUserInput> largeFileReader() {
        // 文件资源
        FileSystemResource resource = new FileSystemResource("data/input/large_users.txt");

        // Header 解析器：yyyyMMdd
        HeaderParser headerParser = line -> {
            LocalDate date = LocalDate.parse(line.trim(), DateTimeFormatter.ofPattern("yyyyMMdd"));
            return new HeaderInfo(date);
        };

        // Header 校验器
        HeaderValidator headerValidator = header -> {
            LocalDate today = LocalDate.now();
            if (!header.getDate().equals(today)) {
                System.out.println("[WARN] Large file header date mismatch: expected=" + today + ", actual=" + header.getDate());
            }
        };

        // Footer 解析器：纯数字
        FooterParser footerParser = line -> {
            long count = Long.parseLong(line.trim());
            return new FooterInfo(count);
        };

        // Footer 校验器：数量匹配
        FooterValidator footerValidator = (footer, actual) -> {
            if (footer.getCount() != actual) {
                throw new IllegalStateException("Large file footer validation failed: expected count=" + footer.getCount() + ", actual count=" + actual);
            }
        };

        // LineTokenizer：逗号分隔
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setNames("name", "age", "email", "birthDate");

        // FieldSetMapper
        BeanWrapperFieldSetMapper<DemoUserInput> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(DemoUserInput.class);

        // 创建 HeaderFooterAwareReader
        return new HeaderFooterAwareReader<>(
                resource,
                headerParser,
                headerValidator,
                footerParser,
                footerValidator,
                lineTokenizer,
                fieldSetMapper
        );
    }


    /**
     * Chunk 执行监听器
     */
    @Bean
    public ChunkListener chunkExecutionListener() {
        return new ChunkListener() {
            @Override
            public void beforeChunk(ChunkContext context) {
                System.out.println("========================================");
                System.out.println("[CHUNK] Starting chunk: " + context.getAttribute("chunk.count"));
            }

            @Override
            public void afterChunk(ChunkContext context) {
                System.out.println("[CHUNK] Chunk completed");
                System.out.println("========================================");
            }
        };
    }


    /**
     * Step 执行监听器实现
     */
    @Bean
    public StepExecutionListener StepExecutionListenerImpl() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                System.out.println("[CHUNK] Step started");
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                System.out.println("[CHUNK] Step completed. Read: " + stepExecution.getReadCount()
                        + ", Written: " + stepExecution.getWriteCount()
                        + ", Skipped: " + stepExecution.getSkipCount());
                return stepExecution.getExitStatus();
            }
        };
    }

// =========== Job5: ComplexWorkflow
    /**
     * Reader：读取 workflow_users.txt
     * <p>
     * 文件格式：yyyyMMdd + 数据行 + count
     * 使用 HeaderFooterAwareReader 进行头尾校验
     */
    @Bean
    public HeaderFooterAwareReader<DemoUserInput> workflowFileReader() {
        // 文件资源
        FileSystemResource resource = new FileSystemResource("data/input/workflow_users.txt");

        // Header 解析器：yyyyMMdd
        HeaderParser headerParser = line -> {
            LocalDate date = LocalDate.parse(line.trim(), DateTimeFormatter.ofPattern("yyyyMMdd"));
            return new HeaderInfo(date);
        };

        // Header 校验器
        HeaderValidator headerValidator = header -> {
            LocalDate today = LocalDate.now();
            if (!header.getDate().equals(today)) {
                System.out.println("[WARN] Workflow file header date mismatch: expected=" + today + ", actual=" + header.getDate());
            }
        };

        // Footer 解析器：纯数字
        FooterParser footerParser = line -> {
            long count = Long.parseLong(line.trim());
            return new FooterInfo(count);
        };

        // Footer 校验器：数量匹配
        FooterValidator footerValidator = (footer, actual) -> {
            if (footer.getCount() != actual) {
                throw new IllegalStateException("Workflow file footer validation failed: expected count=" + footer.getCount() + ", actual count=" + actual);
            }
        };

        // LineTokenizer：逗号分隔
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");
        lineTokenizer.setNames("name", "age", "email", "birthDate");

        // FieldSetMapper
        BeanWrapperFieldSetMapper<DemoUserInput> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(DemoUserInput.class);

        // 创建 HeaderFooterAwareReader
        return new HeaderFooterAwareReader<>(
                resource,
                headerParser,
                headerValidator,
                footerParser,
                footerValidator,
                lineTokenizer,
                fieldSetMapper
        );
    }


// =========== Job1: ConditionalFlow
    /**
     * Reader：读取 demo_users.txt
     * <p>
     * 文件格式：H|yyyyMMdd|type + 数据行 + T|count
     * 使用 HeaderFooterAwareReader 进行头尾校验
     */
    @Bean
    public HeaderFooterAwareReader<DemoUserInput> demoUsersFileReader() {
        // 文件资源
        FileSystemResource resource = new FileSystemResource("data/input/demo_users.txt");

        // Header 解析器：H|yyyyMMdd|type
        HeaderParser headerParser = line -> {
            String[] parts = line.split("\\|");
            if (parts.length < 3) {
                throw new FlatFileParseException("Invalid header format: " + line, line, 1);
            }
            String dateStr = parts[1];  // yyyyMMdd
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return new HeaderInfo(date);
        };

        // Header 校验器：可选，这里不进行严格校验
       HeaderValidator headerValidator = header -> {
            // 可以根据需要添加日期校验逻辑
            LocalDate today = LocalDate.now();
            if (!header.getDate().equals(today)) {
                System.out.println("[WARN] Header date mismatch: expected=" + today + ", actual=" + header.getDate());
            }
        };

        // Footer 解析器：T|count
        FooterParser footerParser = line -> {
            // 去掉 T| 前缀，提取数字
            String countStr = line.substring(line.indexOf("|") + 1);
            long count = Long.parseLong(countStr);
            return new FooterInfo(count);
        };

        // Footer 校验器：数量匹配
        FooterValidator footerValidator = (footer, actual) -> {
            if (footer.getCount() != actual) {
                throw new IllegalStateException("Footer validation failed: expected count=" + footer.getCount() + ", actual count=" + actual);
            }
        };

        // LineTokenizer：使用 | 分隔符
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter("|");
        lineTokenizer.setNames("id", "name", "age", "email", "birthDate");

        // FieldSetMapper：使用 AnnotationDrivenFieldSetMapper 支持 @FileColumn 注解
        AnnotationDrivenFieldSetMapper<DemoUserInput> fieldSetMapper =  new AnnotationDrivenFieldSetMapper<>(DemoUserInput.class);

        // 创建 HeaderFooterAwareReader
        return new HeaderFooterAwareReader<>(
                resource,
                headerParser,
                headerValidator,
                footerParser,
                footerValidator,
                lineTokenizer,
                fieldSetMapper
        );
    }

    /**
     * Footer 行检测器：检测 T| 前缀的 Footer 行
     */
    @Bean
    public FooterLineDetector demoUsersFooterLineDetector() {
        return line -> line != null && line.startsWith("T|");
    }




    @Bean()
    public ItemWriter<DemoUser> db2DemoUserWriter(Db2BusinessService db2BusinessService) {
        return items -> db2BusinessService.batchInsertUsers(new ArrayList<>(items.getItems()));
    }

    @Bean()
    public ItemWriter<DemoUser> db3DemoUserWriter(Db3BusinessService db3BusinessService) {
        return items -> db3BusinessService.batchInsertUsers(new ArrayList<>(items.getItems()));
    }

    @Bean()
    public ItemWriter<DemoUser> db4DemoUserWriter(Db4BusinessService db4BusinessService) {
        return items -> db4BusinessService.batchInsertUsers(new ArrayList<>(items.getItems()));
    }

    @Bean()
    public ItemProcessor<DemoUserInput, DemoUser> demoUserInputToDemoUserCopyIdProcessor() {
        return input -> {
            DemoUser user = new DemoUser();
            user.setId(input.getId());
            user.setName(input.getName());
            user.setEmail(input.getEmail());
            user.setBirthDate(input.getBirthDate());
            return user;
        };
    }

    @Bean()
    public ItemProcessor<DemoUserInput, DemoUser> demoUserInputToDemoUserNoIdProcessor() {
        return input -> {
            DemoUser user = new DemoUser();
            user.setId(null);
            user.setName(input.getName());
            user.setEmail(input.getEmail());
            user.setBirthDate(input.getBirthDate());
            return user;
        };
    }

    /**
     * 导入后校验监听器
     * <p>
     * 校验逻辑：实际处理条数（写入+跳过）必须等于 Footer 声明的记录数
     * <p>
     * 适用场景：
     * - 有头有尾：校验 writeCount + skipCount == declaredCount（允许跳过）
     * - 有头无尾/无头无尾：declaredCount=0，可以选择跳过校验或使用 Job 参数
     */
    @Bean
    public FooterValidationListener footerValidationListener() {
        // 定义校验逻辑
        PostImportValidator validator = (declaredCount, readCount, writeCount, skipCount) -> {
            // 场景1：有Footer，校验总处理数
            if (declaredCount > 0) {
                long totalProcessed = writeCount + skipCount;
                if (totalProcessed != declaredCount) {
                    throw new IllegalStateException(
                            String.format("Import validation failed: Footer expects %d records, " +
                                    "but actually wrote %d + skipped %d = %d (read=%d)",
                                    declaredCount, writeCount, skipCount, totalProcessed, readCount)
                    );
                }
                System.out.println("✅ Import validation passed: Footer=" + declaredCount +
                        ", wrote=" + writeCount + ", skipped=" + skipCount);
            } else {
                // 场景2/3：无Footer，记录日志但不失败
                System.out.println("ℹ️ No footer declaration, skipping count validation. " +
                        "Wrote " + writeCount + " records (read=" + readCount + ", skip=" + skipCount + ")");
            }
        };

        return new FooterValidationListener(validator);
    }
}

