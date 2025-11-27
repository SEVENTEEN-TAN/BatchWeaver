package com.example.batch.chunk;

import org.springframework.batch.item.ItemReader;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

public class CursorUserReader implements ItemReader<Map<String, Object>> {

    private JdbcTemplate jdbcTemplate;
    private int pageSize = 500;
    private int offset = 0;
    private List<Map<String, Object>> buffer;
    private int index = 0;

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public Map<String, Object> read() {
        if (buffer == null || index >= buffer.size()) {
            String sql = "SELECT ID, USERNAME, EMAIL, STATUS FROM DEMO_USER ORDER BY ID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            buffer = jdbcTemplate.queryForList(sql, offset, pageSize);
            offset += pageSize;
            index = 0;
            if (buffer.isEmpty()) return null;
        }
        return buffer.get(index++);
    }
}

