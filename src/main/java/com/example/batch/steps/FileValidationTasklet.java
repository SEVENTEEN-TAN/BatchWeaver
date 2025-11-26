package com.example.batch.steps;

import com.example.batch.components.FileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FileValidationTasklet {

    private static final Logger log = LoggerFactory.getLogger(FileValidationTasklet.class);

    private String filePath;

    @Autowired
    private FileValidator fileValidator;

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void execute() {
        log.info("Validating file: {}", filePath);
        fileValidator.validate(filePath);
    }
}
