package com.batchweaver.core.fileprocess.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件头信息
 */
@Data
public class HeaderInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日期
     */
    private LocalDate date;

    /**
     * 扩展元数据（可选）
     */
    private Map<String, Object> metadata;

    public HeaderInfo() {
        this.metadata = new HashMap<>();
    }

    public HeaderInfo(LocalDate date) {
        this.date = date;
        this.metadata = new HashMap<>();
    }

    public HeaderInfo(LocalDate date, Map<String, Object> metadata) {
        this.date = date;
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    /**
     * 创建空头信息（无头场景）
     */
    public static HeaderInfo empty() {
        return new HeaderInfo();
    }
}
