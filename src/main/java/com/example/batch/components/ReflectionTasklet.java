package com.example.batch.components;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * A generic Tasklet that executes a method on a target bean or class.
 */
public class ReflectionTasklet implements Tasklet, ApplicationContextAware {

    private final String className;
    private final String methodName;
    private final java.util.Map<String, String> properties;
    private ApplicationContext applicationContext;

    public ReflectionTasklet(String className, String methodName, java.util.Map<String, String> properties) {
        this.className = className;
        this.methodName = methodName != null ? methodName : "execute";
        this.properties = properties;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        Object target;
        Class<?> clazz = Class.forName(className);

        // Try to get bean from context first
        try {
            target = applicationContext.getBean(clazz);
        } catch (Exception e) {
            // Fallback to manual instantiation
            target = clazz.getDeclaredConstructor().newInstance();
        }

        // Apply properties
        if (properties != null && !properties.isEmpty()) {
            org.springframework.beans.BeanWrapper wrapper = new org.springframework.beans.BeanWrapperImpl(target);
            properties.forEach(wrapper::setPropertyValue);
        }

        // Find and invoke method
        Method method = ReflectionUtils.findMethod(clazz, methodName);
        if (method == null) {
            method = ReflectionUtils.findMethod(clazz, methodName, StepContribution.class, ChunkContext.class);
            if (method != null) {
                try {
                    method.invoke(target, contribution, chunkContext);
                    return RepeatStatus.FINISHED;
                } catch (java.lang.reflect.InvocationTargetException e) {
                    Throwable cause = e.getTargetException();
                    if (cause instanceof RuntimeException re) throw re;
                    throw new RuntimeException(cause);
                }
            }
            throw new IllegalArgumentException("Method " + methodName + " not found in " + className);
        }

        try {
            method.invoke(target);
            return RepeatStatus.FINISHED;
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException(cause);
        }
    }
}
