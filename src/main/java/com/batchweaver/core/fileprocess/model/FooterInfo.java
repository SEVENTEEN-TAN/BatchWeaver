package com.batchweaver.core.fileprocess.model;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件尾信息
 */
@Data
public class FooterInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录数
     */
    private Long count;

    /**
     * 扩展元数据（可选，如校验和）
     */
    private Map<String, Object> metadata;

    public FooterInfo() {
        this.count = 0L;  // 默认值为 0，避免 NPE
        this.metadata = new HashMap<>();
    }

    public FooterInfo(Long count) {
        this.count = count != null ? count : 0L;
        this.metadata = new HashMap<>();
    }

    public FooterInfo(Long count, Map<String, Object> metadata) {
        this.count = count != null ? count : 0L;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    /**
     * 创建空尾信息（无尾场景）
     */
    public static FooterInfo empty() {
        return new FooterInfo(0L);
    }
}
