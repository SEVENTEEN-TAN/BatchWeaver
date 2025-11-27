package com.example.batch.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
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
    private final JobExplorer jobExplorer;

    public DynamicJobRunner(JobLauncher jobLauncher, XmlJobParser xmlJobParser, JobExplorer jobExplorer) {
        this.jobLauncher = jobLauncher;
        this.xmlJobParser = xmlJobParser;
        this.jobExplorer = jobExplorer;
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
            log.info("Available jobs: {}", xmlJobParser.getAvailableJobNames());
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
            String value = params.getProperty(key);
            if (isNumeric(value)) {
                try {
                    paramsBuilder.addLong(key, Long.parseLong(value));
                } catch (NumberFormatException ex) {
                    paramsBuilder.addString(key, value);
                }
            } else {
                paramsBuilder.addString(key, value);
            }
        }

        if (!params.containsKey("id")) {
            paramsBuilder.addLong("id", System.currentTimeMillis());
            log.info("No id provided, using startup timestamp");
        }

        JobParameters jobParameters = paramsBuilder.toJobParameters();

        try {
            JobExecution execution = jobLauncher.run(job, jobParameters);
            log.info("JobExecution started id={} status={}", execution.getId(), execution.getStatus());
        } catch (JobInstanceAlreadyCompleteException e) {
            log.info("Job already completed for id={}, not re-executing", jobParameters.getParameters().get("id"));
        } catch (JobExecutionAlreadyRunningException e) {
            log.info("Job is already running for id={}, skip duplicate", jobParameters.getParameters().get("id"));
        } catch (Exception e) {
            log.error("Job execution failed", e);
        }
    }

    private boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }
}
