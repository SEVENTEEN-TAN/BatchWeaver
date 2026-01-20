package com.example.batch.core.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ExecutionLoggingAspect {

    // 只拦截 Service 层方法
    @Around("execution(* com.example.batch..service..*(..))")
    public Object aroundServiceMethods(ProceedingJoinPoint pjp) throws Throwable {
        String rid = resolveRunId();
        log.info("Start {} rid={}", pjp.getSignature().toShortString(), rid);
        try {
            Object result = pjp.proceed();
            log.info("End {} rid={}", pjp.getSignature().toShortString(), rid);
            return result;
        } catch (Throwable t) {
            log.error("Error {} rid={}", pjp.getSignature().toShortString(), rid, t);
            throw t;
        }
    }

    private String resolveRunId() {
        var ctx = StepSynchronizationManager.getContext();
        if (ctx != null) {
            var params = ctx.getStepExecution().getJobExecution().getJobParameters();
            var jp = params.getParameters().get("id");
            if (jp != null && jp.getValue() != null) {
                return String.valueOf(jp.getValue());
            }
        }
        return "unknown";
    }
}
