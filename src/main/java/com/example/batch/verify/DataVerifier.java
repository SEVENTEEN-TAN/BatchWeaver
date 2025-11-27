package com.example.batch.verify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DataVerifier implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && args[0].startsWith("jobName=")) {
            return; // Skip verification when running jobs
        }

        log.info("=== Data Verification ===");
        List<Map<String, Object>> results = jdbcTemplate.queryForList("SELECT * FROM USER_DATA");
        log.info("Total records: {}", results.size());
        for (Map<String, Object> row : results) {
            log.info(row.toString());
        }
    }
}
