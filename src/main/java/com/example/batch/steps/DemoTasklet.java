package com.example.batch.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DemoTasklet {

    private static final Logger log = LoggerFactory.getLogger(DemoTasklet.class);

    public void execute() {
        log.info("Executing DemoTasklet...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("DemoTasklet execution finished.");
    }
}
