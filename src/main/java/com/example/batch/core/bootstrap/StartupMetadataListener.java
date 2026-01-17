package com.example.batch.core.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动器前置检查
 * 在所有 CommandLineRunner 中最先执行，检查 Spring Batch 元数据表是否存在
 * 如果检查失败，将抛出异常阻止应用继续运行
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StartupMetadataListener implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String[] TABLES = new String[]{
            "BATCH_JOB_INSTANCE",
            "BATCH_JOB_EXECUTION",
            "BATCH_JOB_EXECUTION_PARAMS",
            "BATCH_STEP_EXECUTION",
            "BATCH_STEP_EXECUTION_CONTEXT",
            "BATCH_JOB_EXECUTION_CONTEXT"
    };

    @Override
    public void run(String... args) {
        printAscii();

        log.info("========================================");
        log.info("Checking Spring Batch metadata tables...");
        log.info("========================================");

        List<String> missingTables = new ArrayList<>();

        for (String table : TABLES) {
            boolean exists = checkTableExists(table);
            if (exists) {
                log.info("✓ metadata table {} exists", table);
            } else {
                log.error("✗ metadata table {} is MISSING", table);
                missingTables.add(table);
            }
        }

        if (!missingTables.isEmpty()) {
            log.error("========================================");
            log.error("FATAL: Missing {} metadata table(s): {}", missingTables.size(), missingTables);
            log.error("========================================");
            log.error("Action required:");
            log.error("  1. Execute database initialization script:");
            log.error("     → Run: scripts/init.sql");
            log.error("  2. Or use Spring Batch schema initialization:");
            log.error("     → Add to application.yml:");
            log.error("       spring.batch.jdbc.initialize-schema: always");
            log.error("========================================");

            throw new IllegalStateException(
                String.format("Spring Batch metadata tables are missing: %s. " +
                    "Please run scripts/init.sql to initialize the database.", missingTables)
            );
        }

        log.info("========================================");
        log.info("All metadata tables verified successfully");
        log.info("========================================");
    }

    private boolean checkTableExists(String table) {
        try {
            jdbcTemplate.queryForObject("SELECT TOP 1 1 FROM " + table, Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void printAscii() {
        String b = "\n  ____        _       _  __        __                        \n" +
                " | __ )  __ _| |_ ___| |_\\ \\      / ___  __ ___   _____ _ __ \n" +
                " |  _ \\ / _` | __/ __| '_ \\ \\ /\\ / / _ \\/ _` \\ \\ / / _ | '__|\n" +
                " | |_) | (_| | || (__| | | \\ V  V |  __| (_| |\\ V |  __| |   \n" +
                " |____/ \\__,_|\\__\\___|_| |_|\\_/\\_/ \\___|\\__,_| \\_/ \\___|_|   \n" +
                "                                                             ";
        log.info(b);
    }
}

