package com.batchweaver.core.validator;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 首行校验器
 *
 * 首行格式：H|20261231|FILE_IDENTIFIER
 * - H：首行标识符
 * - 20261231：日期（yyyyMMdd 格式）
 * - FILE_IDENTIFIER：文件标识符
 */
@Component
public class HeaderValidator {

    private static final String HEADER_PREFIX = "H|";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 校验首行格式
     *
     * @param headerLine 首行内容
     * @throws ValidationException 校验失败时抛出异常
     */
    public void validate(String headerLine) throws ValidationException {
        if (headerLine == null || !headerLine.startsWith(HEADER_PREFIX)) {
            throw new ValidationException("Invalid header format: must start with 'H|'");
        }

        String[] parts = headerLine.split("\\|");
        if (parts.length < 3) {
            throw new ValidationException("Invalid header format: missing fields (expected format: H|yyyyMMdd|fileId)");
        }

        // 验证日期格式
        String dateStr = parts[1];
        try {
            LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format in header: " + dateStr + " (expected: yyyyMMdd)", e);
        }

        // 验证文件标识
        String fileIdentifier = parts[2];
        if (fileIdentifier == null || fileIdentifier.trim().isEmpty()) {
            throw new ValidationException("Invalid file identifier in header: cannot be empty");
        }
    }

    /**
     * 提取首行日期
     */
    public LocalDate extractDate(String headerLine) {
        String[] parts = headerLine.split("\\|");
        return LocalDate.parse(parts[1], DATE_FORMATTER);
    }

    /**
     * 提取文件标识符
     */
    public String extractFileIdentifier(String headerLine) {
        String[] parts = headerLine.split("\\|");
        return parts[2];
    }
}
