package com.example.batch.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Step2Processor {

    private static final Logger log = LoggerFactory.getLogger(Step2Processor.class);

    public void execute() {
        log.info("=== Step 2: 数据转换 ===");
        // 这里执行第二步的业务逻辑
        // 例如：数据转换、字段映射等
        log.info("Step 2 执行完成");
    }
}
