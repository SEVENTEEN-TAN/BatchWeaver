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
            paramsBuilder.addString(key, params.getProperty(key));
        }
        
        // 断点续传机制说明：
        // 1. Spring Batch 会自动识别失败的 Job（通过 JobParameters 匹配）
        // 2. 如果使用相同的参数运行，会自动从失败的 Step 继续执行
        // 3. 如果需要强制创建新实例，传入 restart=false 参数
        boolean forceNewInstance = "false".equalsIgnoreCase(params.getProperty("restart"));
        
        if (forceNewInstance) {
            // 添加时间戳，强制创建新的 Job 实例
            paramsBuilder.addLong("run.id", System.currentTimeMillis());
            log.info("Force creating new job instance (restart=false)");
        } else {
            // 使用固定参数，支持断点续传
            // 如果之前有失败的实例，Spring Batch 会自动重启
            if (!params.containsKey("run.id")) {
                paramsBuilder.addString("run.id", "default");
            }
            log.info("Using consistent parameters to support automatic restart");
        }

        try {
            jobLauncher.run(job, paramsBuilder.toJobParameters());
        } catch (Exception e) {
            log.error("Job execution failed", e);
        }
    }
}
