package com.batchweaver.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置文件解密处理器 - 在 Spring Boot 启动前解密配置中的敏感信息
 * <p>
 * 执行时机：Spring Boot 启动早期，在 Environment 创建之后、Bean 创建之前
 * <p>
 * 加密格式：
 * <pre>
 * spring.datasource.password=SM4(encrypted_password_here)
 * </pre>
 * <p>
 * 注册方式：在 META-INF/spring.factories 中注册
 * <p>
 * 注意：
 * - 此处理器在 Spring Boot 启动前执行，无法使用 @Autowired 注入 Bean
 * - 解密算法由调用方实现，本类只负责提取 SM4() 中的值并调用解密方法
 */
public class DecryptEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DecryptEnvironmentPostProcessor.class);

    private static final String PROPERTY_SOURCE_NAME = "decrypted-properties";

    // SM4 加密标识
    private static final String SM4_PREFIX = "SM4(";
    private static final String SM4_SUFFIX = ")";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        log.info("DecryptEnvironmentPostProcessor: Checking for SM4 encrypted properties...");

        Map<String, Object> decryptedProperties = new HashMap<>();

        // 遍历所有 PropertySource
        environment.getPropertySources().forEach(propertySource -> {
            if (propertySource.getSource() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sourceMap = (Map<String, Object>) propertySource.getSource();

                sourceMap.forEach((key, value) -> {
                    if (value instanceof String strValue) {
                        String decryptedValue = decryptIfEncrypted(key, strValue);
                        if (!decryptedValue.equals(strValue)) {
                            log.info("Decrypted property: {}", key);
                            decryptedProperties.put(key, decryptedValue);
                        }
                    }
                });
            }
        });

        // 将解密后的属性添加到 Environment 的最前面（优先级最高）
        if (!decryptedProperties.isEmpty()) {
            environment.getPropertySources().addFirst(
                new MapPropertySource(PROPERTY_SOURCE_NAME, decryptedProperties)
            );
            log.info("Total {} properties decrypted.", decryptedProperties.size());
        } else {
            log.debug("No SM4 encrypted properties found.");
        }
    }

    /**
     * 检查并解密属性值
     *
     * @param key   属性键
     * @param value 属性值
     * @return 解密后的值，如果未加密则返回原值
     */
    private String decryptIfEncrypted(String key, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // 检查 SM4(encrypted_value) 格式
        if (value.startsWith(SM4_PREFIX) && value.endsWith(SM4_SUFFIX)) {
            String encryptedValue = value.substring(
                SM4_PREFIX.length(),
                value.length() - SM4_SUFFIX.length()
            );
            return decrypt(encryptedValue);
        }

        return value;
    }

    /**
     * 解密方法 - 由调用方实现
     * <p>
     * TODO: 实现 SM4 解密逻辑
     * <p>
     * 实现示例：
     * <pre>
     * private String decrypt(String encryptedValue) {
     *     // 方式1: 使用 SM4 工具类
     *     return SM4Util.decrypt(encryptedValue);
     * }
     * </pre>
     *
     * @param encryptedValue 加密的值
     * @return 解密后的值
     */
    private String decrypt(String encryptedValue) {
        // TODO: 实现 SM4 解密
        // return SM4Util.decrypt(encryptedValue);

        // 临时：直接返回原值（解密实现后删除此行）
        return encryptedValue;
    }
}
