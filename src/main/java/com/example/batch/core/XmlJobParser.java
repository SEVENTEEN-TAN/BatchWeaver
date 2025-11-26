package com.example.batch.core;

import com.example.batch.components.ReflectionTasklet;
import com.example.batch.core.model.JobXml;
import com.example.batch.core.model.StepXml;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class XmlJobParser {

    private static final Logger log = LoggerFactory.getLogger(XmlJobParser.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ApplicationContext applicationContext;

    private final Map<String, Job> jobRegistry = new ConcurrentHashMap<>();
    private final ObjectMapper xmlMapper = new XmlMapper();

    public XmlJobParser(JobRepository jobRepository, PlatformTransactionManager transactionManager, ApplicationContext applicationContext) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void loadJobs() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:jobs/*.xml");

            for (Resource resource : resources) {
                try {
                    JobXml jobXml = xmlMapper.readValue(resource.getInputStream(), JobXml.class);
                    Job job = buildJob(jobXml);
                    jobRegistry.put(job.getName(), job);
                    log.info("Registered job: {}", job.getName());
                } catch (Exception e) {
                    log.error("Failed to parse job from resource: {}", resource.getFilename(), e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to load job resources", e);
        }
    }

    private Job buildJob(JobXml jobXml) {
        JobBuilder jobBuilder = new JobBuilder(jobXml.getId(), jobRepository);
        
        // Simple sequential steps for now
        if (jobXml.getSteps() == null || jobXml.getSteps().isEmpty()) {
            throw new IllegalArgumentException("Job " + jobXml.getId() + " has no steps");
        }

        org.springframework.batch.core.job.builder.SimpleJobBuilder simpleJobBuilder = null;

        for (StepXml stepXml : jobXml.getSteps()) {
            Step step = buildStep(stepXml);
            if (simpleJobBuilder == null) {
                simpleJobBuilder = jobBuilder.start(step);
            } else {
                simpleJobBuilder.next(step);
            }
        }

        return simpleJobBuilder.build();
    }

    private Step buildStep(StepXml stepXml) {
        java.util.Map<String, String> properties = new java.util.HashMap<>();
        if (stepXml.getProperties() != null) {
            stepXml.getProperties().forEach(p -> properties.put(p.getName(), p.getValue()));
        }

        ReflectionTasklet tasklet = new ReflectionTasklet(stepXml.getClassName(), stepXml.getMethodName(), properties);
        tasklet.setApplicationContext(applicationContext);

        return new StepBuilder(stepXml.getId(), jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    public Job getJob(String jobName) {
        return jobRegistry.get(jobName);
    }

    /**
     * 获取所有可用的 Job 名称
     * @return Job 名称集合
     */
    public java.util.Set<String> getAvailableJobNames() {
        return jobRegistry.keySet();
    }
}
