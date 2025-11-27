package com.example.batch.core;

import com.example.batch.components.ReflectionTasklet;
import com.example.batch.core.model.JobXml;
import com.example.batch.core.model.StepXml;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class XmlJobParser {

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
        // chunk step
        if ((stepXml.getType() != null && stepXml.getType().equalsIgnoreCase("chunk"))
                || stepXml.getReaderClass() != null) {
            int commitInterval = stepXml.getCommitInterval() != null ? stepXml.getCommitInterval() : 500;
            org.springframework.batch.item.ItemReader<?> reader = (org.springframework.batch.item.ItemReader<?>) instantiate(stepXml.getReaderClass());
            org.springframework.batch.item.ItemProcessor<?, ?> processor = null;
            org.springframework.batch.item.ItemWriter<?> writer = (org.springframework.batch.item.ItemWriter<?>) instantiate(stepXml.getWriterClass());
            if (stepXml.getProcessorClass() != null) {
                processor = (org.springframework.batch.item.ItemProcessor<?, ?>) instantiate(stepXml.getProcessorClass());
            }

            // optional injection
            injectIfPresent(reader, "setJdbcTemplate", org.springframework.jdbc.core.JdbcTemplate.class);
            injectIfPresent(writer, "setJdbcTemplate", org.springframework.jdbc.core.JdbcTemplate.class);
            if (stepXml.getPageSize() != null) {
                injectIfPresent(reader, "setPageSize", Integer.class, stepXml.getPageSize());
            }

            StepBuilder sb = new StepBuilder(stepXml.getId(), jobRepository);
            org.springframework.batch.core.step.builder.SimpleStepBuilder<?, ?> ssb = sb.<Object, Object>chunk(commitInterval, transactionManager)
                    .reader((org.springframework.batch.item.ItemReader) reader)
                    .writer((org.springframework.batch.item.ItemWriter) writer);
            if (processor != null) {
                ssb.processor((org.springframework.batch.item.ItemProcessor) processor);
            }
            return ssb.build();
        }

        // tasklet step (default)
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

    private Object instantiate(String className) {
        if (className == null || className.isEmpty()) return null;
        try {
            Class<?> clazz = Class.forName(className);
            try {
                return applicationContext.getBean(clazz);
            } catch (Exception e) {
                return clazz.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate: " + className, e);
        }
    }

    private void injectIfPresent(Object target, String methodName, Class<?> argType) {
        if (target == null) return;
        try {
            java.lang.reflect.Method m = target.getClass().getMethod(methodName, argType);
            Object bean = applicationContext.getBean(argType);
            m.invoke(target, bean);
        } catch (Exception ignored) {}
    }

    private void injectIfPresent(Object target, String methodName, Class<?> argType, Object value) {
        if (target == null) return;
        try {
            java.lang.reflect.Method m = target.getClass().getMethod(methodName, argType);
            m.invoke(target, value);
        } catch (Exception ignored) {}
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
