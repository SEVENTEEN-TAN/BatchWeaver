package com.example.batch.verify;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataVerifier implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0 && args[0].startsWith("jobName=")) {
            return; // Skip verification when running jobs
        }

        System.out.println("=== Data Verification ===");
        List<Map<String, Object>> results = jdbcTemplate.queryForList("SELECT * FROM USER_DATA");
        System.out.println("Total records: " + results.size());
        for (Map<String, Object> row : results) {
            System.out.println(row);
        }
    }
}
