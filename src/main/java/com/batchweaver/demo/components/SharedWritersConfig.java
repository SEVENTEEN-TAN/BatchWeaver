package com.batchweaver.demo.components;

import com.batchweaver.demo.entity.DemoUser;
import com.batchweaver.demo.service.Db2BusinessService;
import com.batchweaver.demo.service.Db3BusinessService;
import com.batchweaver.demo.service.Db4BusinessService;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

/**
 * 共享 Writer 配置
 * <p>
 * 包含多个 Job 共用的 ItemWriter Bean
 *
 * @author BatchWeaver Team
 * @since 1.0.0
 */
@Configuration
public class SharedWritersConfig {

    /**
     * DB2 用户写入器
     * <p>
     * 将用户数据批量写入 DB2 数据库
     */
    @Bean
    public ItemWriter<DemoUser> db2DemoUserWriter(Db2BusinessService db2BusinessService) {
        return items -> db2BusinessService.batchInsertUsers(new ArrayList<>(items.getItems()));
    }

    /**
     * DB3 用户写入器
     * <p>
     * 将用户数据批量写入 DB3 数据库
     */
    @Bean
    public ItemWriter<DemoUser> db3DemoUserWriter(Db3BusinessService db3BusinessService) {
        return items -> db3BusinessService.batchInsertUsers(new ArrayList<>(items.getItems()));
    }

    /**
     * DB4 用户写入器
     * <p>
     * 将用户数据批量写入 DB4 数据库
     */
    @Bean
    public ItemWriter<DemoUser> db4DemoUserWriter(Db4BusinessService db4BusinessService) {
        return items -> db4BusinessService.batchInsertUsers(new ArrayList<>(items.getItems()));
    }
}
