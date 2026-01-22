package com.batchweaver.demo.components;

import com.batchweaver.demo.entity.ChunkUserInput;
import com.batchweaver.demo.entity.DemoUser;
import com.batchweaver.demo.entity.DemoUserInput;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 共享 Processor 配置
 * <p>
 * 包含多个 Job 共用的 ItemProcessor Bean
 *
 * @author BatchWeaver Team
 * @since 1.0.0
 */
@Configuration
public class SharedProcessorsConfig {

    /**
     * DemoUserInput 转 DemoUser 处理器（保留 ID）
     * <p>
     * 将输入实体转换为输出实体，保留原始 ID
     */
    @Bean
    public ItemProcessor<DemoUserInput, DemoUser> demoUserInputToDemoUserCopyIdProcessor() {
        return input -> {
            DemoUser user = new DemoUser();
            user.setId(input.getId());
            user.setName(input.getName());
            user.setEmail(input.getEmail());
            user.setBirthDate(input.getBirthDate());
            return user;
        };
    }

    /**
     * ChunkUserInput 转 DemoUser 处理器（不保留 ID）
     * <p>
     * 将输入实体转换为输出实体，不保留 ID（由数据库自动生成）
     */
    @Bean
    public ItemProcessor<ChunkUserInput, DemoUser> demoUserInputToDemoUserNoIdProcessor() {
        return input -> {
            DemoUser user = new DemoUser();
            // 不设置 ID，让数据库自动生成
            user.setName(input.getName());
            user.setEmail(input.getEmail());
            user.setBirthDate(input.getBirthDate());
            return user;
        };
    }
}
