package com.batchweaver.demo.common;

import com.batchweaver.core.fileprocess.function.FooterParser;
import com.batchweaver.core.fileprocess.function.FooterValidator;
import com.batchweaver.core.fileprocess.function.HeaderParser;
import com.batchweaver.core.fileprocess.function.HeaderValidator;
import com.batchweaver.core.fileprocess.model.FooterInfo;
import com.batchweaver.core.fileprocess.model.HeaderInfo;
import com.batchweaver.core.fileprocess.reader.FooterLineDetector;
import com.batchweaver.core.fileprocess.reader.HeaderFooterAwareReader;
import com.batchweaver.core.reader.AnnotationDrivenFieldSetMapper;
import com.batchweaver.demo.entity.ChunkUserInput;
import com.batchweaver.demo.entity.DemoUserInput;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
public class DemoCommonBatchBeans {


    /**
     * Reader：读取 large_users.txt
     * <p>
     * 文件格式：yyyyMMdd + 数据行 + count
     * 使用 HeaderFooterAwareReader 进行头尾校验
     */
    @Bean
    public HeaderFooterAwareReader<ChunkUserInput> largeFileReader() {
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

        // Footer 校验器：不在这里校验,而是在 postValidationStep 中校验
        // 这样可以在所有 chunk 提交后进行校验,如果失败可以回滚所有数据
        FooterValidator footerValidator = null;

        // LineTokenizer：逗号分隔
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setDelimiter(",");

        // FieldSetMapper：使用 AnnotationDrivenFieldSetMapper 支持日期格式转换
        AnnotationDrivenFieldSetMapper<ChunkUserInput> fieldSetMapper = new AnnotationDrivenFieldSetMapper<>(ChunkUserInput.class);

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
        lineTokenizer.setNames("id","name", "age", "email", "birthDate");

        // FieldSetMapper
        AnnotationDrivenFieldSetMapper<DemoUserInput> fieldSetMapper = new AnnotationDrivenFieldSetMapper<>(DemoUserInput.class);

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




}

