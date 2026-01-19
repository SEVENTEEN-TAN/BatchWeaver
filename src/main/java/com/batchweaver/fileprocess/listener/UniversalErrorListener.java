package com.batchweaver.fileprocess.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用错误监听器
 * <p>
 * 适用于所有Job场景（文件、数据库、API等），通过结构化日志记录错误信息
 */
@Slf4j
public class UniversalErrorListener implements SkipListener<Object, Object> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onSkipInRead(Throwable t) {
        StepExecution stepExecution = getStepExecution();

        Map<String, Object> errorLog = new HashMap<>();
        errorLog.put("timestamp", System.currentTimeMillis());
        errorLog.put("job_name", stepExecution.getJobExecution().getJobInstance().getJobName());
        errorLog.put("job_execution_id", stepExecution.getJobExecution().getId());
        errorLog.put("step_name", stepExecution.getStepName());
        errorLog.put("error_source", "READ");
        errorLog.put("item_index", stepExecution.getReadCount());
        errorLog.put("error_type", t.getClass().getSimpleName());
        errorLog.put("error_message", t.getMessage());

        log.error("Skip in READ: {}", toJson(errorLog), t);
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        StepExecution stepExecution = getStepExecution();

        Map<String, Object> errorLog = new HashMap<>();
        errorLog.put("timestamp", System.currentTimeMillis());
        errorLog.put("job_name", stepExecution.getJobExecution().getJobInstance().getJobName());
        errorLog.put("job_execution_id", stepExecution.getJobExecution().getId());
        errorLog.put("step_name", stepExecution.getStepName());
        errorLog.put("error_source", "WRITE");
        errorLog.put("item_index", stepExecution.getWriteCount());
        errorLog.put("data_preview", toPreview(item, 500));
        errorLog.put("key_fields", extractKeyFields(item));
        errorLog.put("error_type", t.getClass().getSimpleName());
        errorLog.put("error_message", t.getMessage());

        log.error("Skip in WRITE: {}", toJson(errorLog), t);
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        StepExecution stepExecution = getStepExecution();

        Map<String, Object> errorLog = new HashMap<>();
        errorLog.put("timestamp", System.currentTimeMillis());
        errorLog.put("job_name", stepExecution.getJobExecution().getJobInstance().getJobName());
        errorLog.put("job_execution_id", stepExecution.getJobExecution().getId());
        errorLog.put("step_name", stepExecution.getStepName());
        errorLog.put("error_source", "PROCESS");
        errorLog.put("item_index", stepExecution.getReadCount());
        errorLog.put("data_preview", toPreview(item, 500));
        errorLog.put("key_fields", extractKeyFields(item));
        errorLog.put("error_type", t.getClass().getSimpleName());
        errorLog.put("error_message", t.getMessage());

        log.error("Skip in PROCESS: {}", toJson(errorLog), t);
    }

    private StepExecution getStepExecution() {
        return StepSynchronizationManager.getContext().getStepExecution();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String toPreview(Object item, int maxLength) {
        if (item == null) {
            return null;
        }
        String json = toJson(item);
        return json.length() > maxLength ? json.substring(0, maxLength) + "..." : json;
    }

    private Map<String, Object> extractKeyFields(Object item) {
        Map<String, Object> keyFields = new HashMap<>();
        if (item == null) {
            return keyFields;
        }

        // 使用反射提取关键字段（id、name等）
        try {
            Class<?> clazz = item.getClass();

            // 尝试获取id字段
            try {
                var idField = clazz.getDeclaredField("id");
                idField.setAccessible(true);
                keyFields.put("id", idField.get(item));
            } catch (NoSuchFieldException ignored) {
            }

            // 尝试获取name字段
            try {
                var nameField = clazz.getDeclaredField("name");
                nameField.setAccessible(true);
                keyFields.put("name", nameField.get(item));
            } catch (NoSuchFieldException ignored) {
            }
        } catch (Exception e) {
            log.debug("Failed to extract key fields", e);
        }

        return keyFields;
    }
}
