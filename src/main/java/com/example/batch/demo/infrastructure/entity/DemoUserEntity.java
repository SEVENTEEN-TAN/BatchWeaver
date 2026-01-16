package com.example.batch.demo.infrastructure.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类（通用）
 *
 * 注意：此实体可用于 4 个数据库中的 DEMO_USER 表
 * 具体使用哪个数据库由 Mapper 或 Service 层的 @UseDataSource 注解控制
 */
@Data
@Table(value = "DEMO_USER")
public class DemoUserEntity {

    /**
     * 主键 ID（自增）
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态：PENDING, ACTIVE, RECONCILED 等
     */
    private String status;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
