package com.batchweaver.core.id;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

public final class HutoolSnowflakeIncrementer implements DataFieldMaxValueIncrementer {

    private final Snowflake snowflake;

    public HutoolSnowflakeIncrementer(long workerId, long datacenterId) {
        this.snowflake = IdUtil.getSnowflake(workerId, datacenterId);
    }

    @Override
    public long nextLongValue() {
        return snowflake.nextId();
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
