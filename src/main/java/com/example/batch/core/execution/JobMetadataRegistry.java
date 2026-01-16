package com.example.batch.core.execution;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Job 元数据注册表
 * 扫描并收集所有标注了 @BatchJob 注解的配置类
 */
@Slf4j
@Service
public class JobMetadataRegistry {

    private final Map<String, JobMetadata> registry = new HashMap<>();
    private final ApplicationContext context;

    public JobMetadataRegistry(ApplicationContext context) {
        this.context = context;
        
        // 扫描所有带 @BatchJob 注解的 Bean
        Map<String, Object> beans = context.getBeansWithAnnotation(BatchJob.class);
        
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();
            
            // 使用 Spring 工具类获取用户定义的类（处理 CGLIB/JDK 代理）
            Class<?> userClass = ClassUtils.getUserClass(bean.getClass());
            
            BatchJob annotation = userClass.getAnnotation(BatchJob.class);
            if (annotation != null) {
                String jobName = annotation.name();
                
                // 校验 Job Bean 是否存在
                if (!isJobBeanExists(jobName)) {
                    log.warn("@BatchJob annotation declares job '{}' but no Job bean with that name exists. " +
                        "Ensure the @Bean method name or explicit name matches.", jobName);
                }
                
                JobMetadata metadata = new JobMetadata(
                    jobName,
                    Arrays.asList(annotation.steps()),
                    annotation.conditionalFlow()
                );
                registry.put(jobName, metadata);
                log.info("Registered job metadata: {} (steps={}, conditionalFlow={})",
                    jobName, metadata.steps(), metadata.conditionalFlow());
            }
        }
        
        log.info("JobMetadataRegistry initialized with {} jobs: {}", 
            registry.size(), registry.keySet());
    }

    /**
     * 检查 Job Bean 是否存在
     */
    private boolean isJobBeanExists(String jobName) {
        try {
            context.getBean(jobName, Job.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 Job 元数据
     *
     * @param jobName Job 名称
     * @return JobMetadata 或 null（如果未注册）
     */
    public JobMetadata get(String jobName) {
        return registry.get(jobName);
    }

    /**
     * 检查 Job 是否已注册
     *
     * @param jobName Job 名称
     * @return true 如果已注册
     */
    public boolean isRegistered(String jobName) {
        return registry.containsKey(jobName);
    }

    /**
     * 获取所有已注册的 Job 名称
     *
     * @return Job 名称集合
     */
    public Set<String> getRegisteredJobNames() {
        return registry.keySet();
    }

    /**
     * 校验 Step 名称是否合法
     *
     * @param jobName Job 名称
     * @param stepName Step 名称
     * @return true 如果合法
     */
    public boolean isValidStep(String jobName, String stepName) {
        JobMetadata metadata = registry.get(jobName);
        return metadata != null && metadata.steps().contains(stepName);
    }

    /**
     * 获取 Job 的所有合法 Step 名称
     *
     * @param jobName Job 名称
     * @return Step 名称列表，如果 Job 未注册返回空列表
     */
    public List<String> getValidSteps(String jobName) {
        return Optional.ofNullable(registry.get(jobName))
            .map(JobMetadata::steps)
            .orElse(List.of());
    }

    /**
     * Job 元数据记录
     */
    public record JobMetadata(
        String name,
        List<String> steps,
        boolean conditionalFlow
    ) {}
}
