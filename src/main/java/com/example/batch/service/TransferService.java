package com.example.batch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TransferService {

    public void step1Produce(StepContribution contribution, ChunkContext chunkContext) {
        var jobExec = chunkContext.getStepContext().getStepExecution().getJobExecution();
        var ctx = jobExec.getExecutionContext();
        var idParam = jobExec.getJobParameters().getLong("id");

        var payload = new TransferPayload();
        payload.setBatchId(String.valueOf(idParam));
        payload.setCount(10);

        ctx.put("payload", payload);
        ctx.put("note", "from-step1");
        log.info("produce payload batchId={} count={}", payload.getBatchId(), payload.getCount());
    }

    public void step2Consume(StepContribution contribution, ChunkContext chunkContext) {
        var jobExec = chunkContext.getStepContext().getStepExecution().getJobExecution();
        var ctx = jobExec.getExecutionContext();

        var payload = (TransferPayload) ctx.get("payload");
        var note = (String) ctx.get("note");

        if (payload != null) {
            log.info("consume payload batchId={} count={} note={}", payload.getBatchId(), payload.getCount(), note);
            payload.setCount(payload.getCount() + 5);
            ctx.put("payload", payload);
            log.info("payload updated count={}", payload.getCount());
        }

        ctx.put("consumed", Boolean.TRUE);
    }
}

