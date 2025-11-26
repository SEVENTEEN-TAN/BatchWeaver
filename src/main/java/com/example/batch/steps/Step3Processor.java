package com.example.batch.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Step3Processor {

    private static final Logger log = LoggerFactory.getLogger(Step3Processor.class);

    public void execute() {
        log.info("=== Step 3: 数据加载 ===");
        // 这里执行第三步的业务逻辑
        // 例如：数据入库、文件生成等
        log.info("Step 3 执行完成");
    }
}
