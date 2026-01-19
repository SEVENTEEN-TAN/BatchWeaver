package com.batchweaver.fileprocess.listener;

import com.batchweaver.fileprocess.function.*;
import com.batchweaver.fileprocess.model.FooterInfo;
import com.batchweaver.fileprocess.model.HeaderInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 头尾校验监听器
 * <p>
 * 使用Lambda函数式接口，支持灵活的头尾解析和校验逻辑
 */
@Slf4j
public class HeaderFooterListener implements StepExecutionListener {

    private final HeaderParser headerParser;
    private final HeaderValidator headerValidator;
    private final FooterParser footerParser;
    private final FooterValidator footerValidator;
    private final Resource resource;

    private HeaderInfo headerInfo;

    public HeaderFooterListener(Resource resource,
                                 HeaderParser headerParser,
                                 HeaderValidator headerValidator,
                                 FooterParser footerParser,
                                 FooterValidator footerValidator) {
        this.resource = resource;
        this.headerParser = headerParser;
        this.headerValidator = headerValidator;
        this.footerParser = footerParser;
        this.footerValidator = footerValidator;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        if (headerParser == null) {
            log.debug("No header parser configured, skipping header validation");
            return;
        }

        try {
            String headerLine = readFirstLine();
            log.info("Reading header line: {}", headerLine);

            headerInfo = headerParser.parse(headerLine);
            log.info("Parsed header: {}", headerInfo);

            if (headerValidator != null) {
                headerValidator.validate(headerInfo);
                log.info("Header validation passed");
            }
        } catch (Exception e) {
            log.error("Header validation failed", e);
            throw new RuntimeException("Header validation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (footerParser == null) {
            log.debug("No footer parser configured, skipping footer validation");
            return ExitStatus.COMPLETED;
        }

        try {
            String footerLine = readLastLine();
            log.info("Reading footer line: {}", footerLine);

            FooterInfo footerInfo = footerParser.parse(footerLine);
            log.info("Parsed footer: {}", footerInfo);

            if (footerValidator != null) {
                long actualCount = stepExecution.getWriteCount();
                footerValidator.validate(footerInfo, actualCount);
                log.info("Footer validation passed: expected={}, actual={}",
                    footerInfo.getCount(), actualCount);
            }

            return ExitStatus.COMPLETED;
        } catch (Exception e) {
            log.error("Footer validation failed", e);
            return ExitStatus.FAILED.addExitDescription("Footer validation failed: " + e.getMessage());
        }
    }

    private String readFirstLine() throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalStateException("File is empty");
            }
            return line;
        }
    }

    private String readLastLine() throws Exception {
        String lastLine = null;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lastLine = line;
            }
        }
        if (lastLine == null) {
            throw new IllegalStateException("File is empty");
        }
        return lastLine;
    }
}
