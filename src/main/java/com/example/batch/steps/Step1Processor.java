package com.example.batch.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Step1Processor {

    private static final Logger log = LoggerFactory.getLogger(Step1Processor.class);

    public void execute() {
        log.info("=== Step 1: 数据预处理 ===");
        // 这里执行第一步的业务逻辑
        // 例如：数据清洗、格式转换等
        log.info("Step 1 执行完成");
    }
}
