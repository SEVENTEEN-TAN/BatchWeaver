package com.example.batch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Slf4j
@Service
public class BreakpointService {

    private static final String MARKER_FILE = "breakpoint_marker.tmp";

    public void prepare() {
        log.info("Step 1: Preparation completed successfully.");
    }

    public void processWithPotentialFailure() {
        log.info("Step 2: Processing with potential failure...");
        
        File marker = new File(MARKER_FILE);
        
        if (!marker.exists()) {
            try {
                if (marker.createNewFile()) {
                    log.error("Simulating failure! Marker file created at: {}", marker.getAbsolutePath());
                    throw new RuntimeException("Simulated Failure! Fix the issue and restart the job.");
                }
            } catch (IOException e) {
                log.error("Failed to create marker file", e);
            }
        } else {
            log.info("Marker file found. Resuming processing...");
            if (marker.delete()) {
                log.info("Marker file deleted. Failure simulation cleared.");
            }
            log.info("Step 2: Processing completed successfully.");
        }
    }

    public void cleanup() {
        log.info("Step 3: Cleanup completed successfully.");
    }
}
