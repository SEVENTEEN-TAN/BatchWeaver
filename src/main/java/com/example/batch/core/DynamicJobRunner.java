package com.example.batch.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Properties;

@Component
public class DynamicJobRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DynamicJobRunner.class);

    private final JobLauncher jobLauncher;
    private final XmlJobParser xmlJobParser;

    public DynamicJobRunner(JobLauncher jobLauncher, XmlJobParser xmlJobParser) {
        this.jobLauncher = jobLauncher;
        this.xmlJobParser = xmlJobParser;
    }

    @Override
    public void run(String... args) throws Exception {
        String jobName = null;
        Properties params = new Properties();

        for (String arg : args) {
            if (arg.startsWith("jobName=")) {
                jobName = arg.split("=")[1];
            } else if (arg.contains("=")) {
                String[] parts = arg.split("=");
                params.put(parts[0], parts[1]);
            }
        }

        if (jobName == null) {
            log.info("No jobName provided in arguments. Skipping CLI job execution.");
            return;
        }

        Job job = xmlJobParser.getJob(jobName);
        if (job == null) {
            log.error("Job not found: {}", jobName);
            return;
        }

        log.info("Starting job: {}", jobName);
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        
        for (String key : params.stringPropertyNames()) {
            paramsBuilder.addString(key, params.getProperty(key));
        }
        
        // If not resuming, add unique parameter to ensure new instance
        if (!params.containsKey("resume") || !"true".equalsIgnoreCase(params.getProperty("resume"))) {
            paramsBuilder.addLong("run.id", System.currentTimeMillis());
        }

        try {
            jobLauncher.run(job, paramsBuilder.toJobParameters());
        } catch (Exception e) {
            log.error("Job execution failed", e);
        }
    }
}
