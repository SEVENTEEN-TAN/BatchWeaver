package com.example.batch.chunk;

import org.springframework.batch.item.ItemProcessor;

import java.util.Map;

public class UppercaseUsernameProcessor implements ItemProcessor<Map<String, Object>, Map<String, Object>> {
    @Override
    public Map<String, Object> process(Map<String, Object> item) {
        Object name = item.get("USERNAME");
        if (name != null) {
            item.put("USERNAME", String.valueOf(name).toUpperCase());
        }
        return item;
    }
}

