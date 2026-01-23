package com.batchweaver.core.id;

import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import java.time.Instant;

/**
 * 基于时间戳的 ID 生成器
 * <p>
 * 直接使用毫秒时间戳，适用于单实例场景
 */
public final class TimestampIncrementer implements DataFieldMaxValueIncrementer {

    @Override
    public long nextLongValue() {
        return Instant.now().toEpochMilli();
    }

    @Override
    public int nextIntValue() {
        long v = nextLongValue();
        if (v > Integer.MAX_VALUE) {
            throw new IllegalStateException("ID too large for int: " + v);
        }
        return (int) v;
    }

    @Override
    public String nextStringValue() {
        return Long.toString(nextLongValue());
    }
}
