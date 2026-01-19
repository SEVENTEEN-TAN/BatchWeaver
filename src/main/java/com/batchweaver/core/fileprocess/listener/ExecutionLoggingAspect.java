package com.batchweaver.core.fileprocess.listener;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.stereotype.Component;

/**
 * 执行日志切面
 * <p>
 * 自动拦截 service 层方法，记录执行日志并关联 Job Execution ID
 */
@Slf4j
@Aspect
@Component
public class ExecutionLoggingAspect {

    @Around("execution(* com.batchweaver..service..*(..))")
    public Object aroundServiceMethods(ProceedingJoinPoint pjp) throws Throwable {
        String executionId = resolveExecutionId();
        log.info("Start {} executionId={}", pjp.getSignature().toShortString(), executionId);
        try {
            Object result = pjp.proceed();
            log.info("End {} executionId={}", pjp.getSignature().toShortString(), executionId);
            return result;
        } catch (Throwable t) {
            log.error("Error {} executionId={}", pjp.getSignature().toShortString(), executionId, t);
            throw t;
        }
    }

    private String resolveExecutionId() {
        var ctx = StepSynchronizationManager.getContext();
        if (ctx != null) {
            Long executionId = ctx.getStepExecution().getJobExecution().getId();
            return executionId != null ? String.valueOf(executionId) : "unknown";
        }
        return "unknown";
    }
}
