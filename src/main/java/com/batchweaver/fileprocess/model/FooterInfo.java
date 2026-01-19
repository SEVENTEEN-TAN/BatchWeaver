package com.batchweaver.fileprocess.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件尾信息
 */
@Data
public class FooterInfo {

    /**
     * 记录数
     */
    private Long count;

    /**
     * 扩展元数据（可选，如校验和）
     */
    private Map<String, Object> metadata;

    public FooterInfo() {
        this.metadata = new HashMap<>();
    }

    public FooterInfo(Long count) {
        this.count = count;
        this.metadata = new HashMap<>();
    }

    public FooterInfo(Long count, Map<String, Object> metadata) {
        this.count = count;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    /**
     * 创建空尾信息（无尾场景）
     */
    public static FooterInfo empty() {
        return new FooterInfo();
    }
}
