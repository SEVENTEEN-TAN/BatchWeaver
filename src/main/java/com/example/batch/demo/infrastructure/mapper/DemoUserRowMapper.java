package com.example.batch.demo.infrastructure.mapper;

import com.example.batch.demo.infrastructure.entity.DemoUserEntity;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * DemoUserEntity RowMapper
 *
 * 将 ResultSet 映射为 DemoUserEntity 对象
 * 可在所有 JdbcTemplate 查询中复用
 */
public class DemoUserRowMapper implements RowMapper<DemoUserEntity> {

    public static final DemoUserRowMapper INSTANCE = new DemoUserRowMapper();

    @Override
    public DemoUserEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        DemoUserEntity user = new DemoUserEntity();
        user.setId(rs.getLong("ID"));
        user.setUsername(rs.getString("USERNAME"));
        user.setEmail(rs.getString("EMAIL"));
        user.setStatus(rs.getString("STATUS"));

        Timestamp updateTime = rs.getTimestamp("UPDATE_TIME");
        if (updateTime != null) {
            user.setUpdateTime(updateTime.toLocalDateTime());
        }

        return user;
    }
}
