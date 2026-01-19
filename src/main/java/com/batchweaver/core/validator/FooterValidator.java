package com.batchweaver.core.validator;

import org.springframework.stereotype.Component;

/**
 * 尾行校验器
 *
 * 尾行格式：T|1000
 * - T：尾行标识符
 * - 1000：记录总数
 */
@Component
public class FooterValidator {

    private static final String FOOTER_PREFIX = "T|";

    /**
     * 校验尾行格式并验证记录总数
     *
     * @param footerLine 尾行内容
     * @param actualRecordCount 实际读取的记录数
     * @throws ValidationException 校验失败时抛出异常
     */
    public void validate(String footerLine, long actualRecordCount) throws ValidationException {
        if (footerLine == null || !footerLine.startsWith(FOOTER_PREFIX)) {
            throw new ValidationException("Invalid footer format: must start with 'T|'");
        }

        String[] parts = footerLine.split("\\|");
        if (parts.length < 2) {
            throw new ValidationException("Invalid footer format: missing record count (expected format: T|count)");
        }

        long declaredCount;
        try {
            declaredCount = Long.parseLong(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid record count in footer: " + parts[1] + " (must be a number)", e);
        }

        if (declaredCount != actualRecordCount) {
            throw new ValidationException(
                String.format("Record count mismatch: declared=%d, actual=%d", declaredCount, actualRecordCount)
            );
        }
    }

    /**
     * 提取尾行声明的记录总数
     */
    public long extractDeclaredCount(String footerLine) {
        String[] parts = footerLine.split("\\|");
        return Long.parseLong(parts[1].trim());
    }
}
