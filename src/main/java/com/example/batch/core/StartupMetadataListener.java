package com.example.batch.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动器前置检查
 */
@Slf4j
@Component
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
        for (String t : TABLES) {
            boolean ok = exists(t);
            log.info("metadata table {} exists={}", t, ok);
            if (!ok) {
                log.error("metadata table missing: {}. apply scripts/init.sql", t);
            }
        }
    }

    private boolean exists(String table) {
        try {
            jdbcTemplate.queryForObject("SELECT TOP 1 1 FROM " + table, Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void printAscii() {
        String b = "\n" +
                "  .   ____          _            __ _ _\n" +
                " /\\ / ___'_ __ _ _(_)_ __  __ _ \\\\ \\\\ \\\n" +
                "( ( )\\___ | '_ | '_| | '_ \\_/ _` | \\ \\ \\ \\ \n" +
                " \\/  ___)| |_)| | | | | || (_| |  ) ) ) )\n" +
                "  '  |____| .__|_| |_|_| |_\\__, | / / / /\n" +
                " =========|_|==============|___/=/_/_/_/\n";
        log.info(b);
    }
}

