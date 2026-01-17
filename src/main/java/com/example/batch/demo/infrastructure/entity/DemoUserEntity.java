package com.example.batch.demo.infrastructure.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类（通用）
 *
 * 用于 4 个数据库中的 DEMO_USER 表
 * 通过 JdbcTemplate + RowMapper 进行 ORM 映射
 */
@Data
public class DemoUserEntity {

    private Long id;
    private String username;
    private String email;
    private String status;
    private LocalDateTime updateTime;
}
