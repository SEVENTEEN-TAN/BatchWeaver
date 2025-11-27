package com.example.batch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DemoService {

    private final JdbcTemplate jdbcTemplate;

    public DemoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void importData() {
        log.info("Starting data import...");
        // Clear existing data for demo purposes
        jdbcTemplate.update("TRUNCATE TABLE DEMO_USER");
        
        for (int i = 1; i <= 10; i++) {
            String sql = "INSERT INTO DEMO_USER (USERNAME, EMAIL, STATUS) VALUES (?, ?, ?)";
            jdbcTemplate.update(sql, "User" + i, "user" + i + "@example.com", "PENDING");
        }
        log.info("Imported 10 users.");
    }

    public void updateData() {
        log.info("Starting data update...");
        String sql = "UPDATE DEMO_USER SET STATUS = ?, UPDATE_TIME = GETDATE() WHERE STATUS = ?";
        int updated = jdbcTemplate.update(sql, "ACTIVE", "PENDING");
        log.info("Updated {} users from PENDING to ACTIVE.", updated);
    }

    public void exportData() {
        log.info("Starting data export...");
        String sql = "SELECT * FROM DEMO_USER";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql);
        
        log.info("Exporting {} users:", users.size());
        for (Map<String, Object> user : users) {
            log.info("User: ID={}, Name={}, Status={}", user.get("ID"), user.get("USERNAME"), user.get("STATUS"));
        }
        log.info("Data export completed.");
    }
}
