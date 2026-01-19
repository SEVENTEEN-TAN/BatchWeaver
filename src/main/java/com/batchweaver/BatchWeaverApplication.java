package com.batchweaver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Batch 5.x Multi-Datasource System
 * <p>
 * 核心特性：
 * - 4 个独立数据源（db1-db4）
 * - 元数据事务（tm1）与业务事务（tm2/tm3/tm4）隔离
 * - 基于注解的 FlatFile 处理框架
 */
@SpringBootApplication
public class BatchWeaverApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchWeaverApplication.class, args);
    }
}
