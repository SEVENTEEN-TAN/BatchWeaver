package com.example.batch.service;

import java.io.Serializable;

public class TransferPayload implements Serializable {
    private String batchId;
    private int count;

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

