package com.batchweaver.core.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * CSV 注入防护工具
 * <p>
 * 检测并转义危险字符，防止 CSV 注入攻击
 * 危险字符：=、+、-、@ 开头的内容
 */
@Component
public class CsvInjectionSanitizer {

    private static final Pattern DANGEROUS_PREFIX = Pattern.compile("^[=+\\-@]");

    /**
     * 转义单个值
     *
     * @param value 原始值
     * @return 转义后的值
     */
    public String sanitize(String value) {
        if (value == null) {
            return null;
        }

        // 检测危险字符开头
        if (DANGEROUS_PREFIX.matcher(value).find()) {
            return "'" + value;  // 在前面加单引号，转义危险字符
        }

        return value;
    }

    /**
     * 批量转义
     */
    public List<String> sanitizeAll(List<String> values) {
        return values.stream()
                .map(this::sanitize)
                .collect(Collectors.toList());
    }
}
