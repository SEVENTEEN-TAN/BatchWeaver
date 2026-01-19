package com.batchweaver.core.util;

import org.springframework.stereotype.Component;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 文件路径规范化和安全校验工具
 *
 * 防止路径遍历攻击和非法文件访问
 */
@Component
public class FilePathNormalizer {

    private static final Pattern PATH_TRAVERSAL = Pattern.compile("\\.\\.");

    /**
     * 规范化文件路径并进行安全校验
     *
     * @param filePath 原始文件路径
     * @return 规范化后的文件路径
     * @throws SecurityException 检测到安全威胁时抛出异常
     */
    public String normalize(String filePath) throws SecurityException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }

        // 防止路径遍历攻击（禁止 ".."）
        if (PATH_TRAVERSAL.matcher(filePath).find()) {
            throw new SecurityException("Path traversal detected: " + filePath);
        }

        // 禁止绝对路径（可选，根据需求调整）
        Path path = Paths.get(filePath);
        if (path.isAbsolute()) {
            throw new SecurityException("Absolute path not allowed: " + filePath);
        }

        // 规范化路径
        try {
            return path.normalize().toString();
        } catch (InvalidPathException e) {
            throw new SecurityException("Invalid file path: " + filePath, e);
        }
    }

    /**
     * 校验文件扩展名
     *
     * @param filePath 文件路径
     * @param allowedExtensions 允许的扩展名集合（如 ["txt", "csv", "dat"]）
     * @throws SecurityException 扩展名不在白名单时抛出异常
     */
    public void validateExtension(String filePath, Set<String> allowedExtensions) {
        String extension = getFileExtension(filePath);
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new SecurityException("File extension not allowed: " + extension);
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        return lastDotIndex > 0 ? filePath.substring(lastDotIndex + 1) : "";
    }
}
