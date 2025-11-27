package com.example.batch.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 环境变量解密
 */
@Slf4j
@Component
public class EnvDecryptProcessor implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // String url = environment.getProperty("spring.datasource.url", "");
        // String username = environment.getProperty("spring.datasource.username", "");
        // String password = environment.getProperty("spring.datasource.password", "");
        // log.info("env datasource url={}", url);
        // log.info("env datasource username={}", username);
        // log.info("env datasource password(masked)={}", mask(password));
    }

    // private String mask(String s) {
    //     if (s == null || s.isEmpty()) return "";
    //     if (s.length() <= 2) return "**";
    //     return s.charAt(0) + "***" + s.charAt(s.length() - 1);
    // }
}

