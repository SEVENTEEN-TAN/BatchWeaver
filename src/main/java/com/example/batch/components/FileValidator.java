package com.example.batch.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class FileValidator {

    private static final Logger log = LoggerFactory.getLogger(FileValidator.class);

    public void validate(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                throw new IllegalArgumentException("File is empty");
            }

            // Validate Header (Date)
            try {
                LocalDate.parse(firstLine.trim(), DateTimeFormatter.ISO_DATE); // YYYY-MM-DD
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid header date format. Expected YYYY-MM-DD, got: " + firstLine);
            }

            String lastLine = firstLine;
            String currentLine;
            int dataCount = 0;

            while ((currentLine = reader.readLine()) != null) {
                lastLine = currentLine;
                dataCount++;
            }
            
            // Adjust count: dataCount includes the last line (footer)
            // So actual data lines = dataCount - 1
            if (dataCount > 0) {
                dataCount--; 
            }

            // Validate Footer (Total count)
            // Expected format: "Total: N" or just "N"
            int declaredCount = parseFooterCount(lastLine);
            
            if (declaredCount != dataCount) {
                throw new IllegalArgumentException("Data count mismatch. Declared: " + declaredCount + ", Actual: " + dataCount);
            }

            log.info("File validation passed: {}", filePath);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    private int parseFooterCount(String footer) {
        try {
            String countStr = footer.replace("Total:", "").trim();
            return Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid footer format. Expected 'Total: N' or 'N', got: " + footer);
        }
    }
}
