package com.example.batch.chunk;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.Chunk;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

public class UserUpdateWriter implements ItemWriter<Map<String, Object>> {

    private JdbcTemplate jdbcTemplate;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(Chunk<? extends Map<String, Object>> chunk) {
        List<? extends Map<String, Object>> items = chunk.getItems();
        if (items == null || items.isEmpty()) return;
        String sql = "UPDATE DEMO_USER SET USERNAME = ?, STATUS = 'ACTIVE' WHERE ID = ?";
        jdbcTemplate.batchUpdate(sql, items, items.size(), (ps, item) -> {
            ps.setString(1, String.valueOf(item.get("USERNAME")));
            ps.setObject(2, item.get("ID"));
        });
    }
}
