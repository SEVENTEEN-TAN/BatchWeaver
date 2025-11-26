package com.example.batch.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@Component
public class CsvImportTasklet {

    private static final Logger log = LoggerFactory.getLogger(CsvImportTasklet.class);

    private String filePath;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void execute() {
        log.info("Importing CSV file: {}", filePath);
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Skip Header (Date)
            reader.readLine();
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Total:")) {
                    break; // Footer reached
                }
                
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String name = parts[0].trim();
                    String email = parts[1].trim();
                    
                    jdbcTemplate.update("INSERT INTO USER_DATA (NAME, EMAIL) VALUES (?, ?)", name, email);
                }
            }
            log.info("CSV import completed.");
        } catch (IOException e) {
            throw new RuntimeException("Failed to import CSV: " + filePath, e);
        }
    }
}
