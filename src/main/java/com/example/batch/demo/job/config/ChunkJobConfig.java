package com.example.batch.demo.job.config;

import com.example.batch.demo.job.service.chunk.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
public class ChunkJobConfig {

    @Bean
    public Job chunkJob(JobRepository jobRepository, Step chunkStep) {
        return new JobBuilder("chunkJob", jobRepository)
                .start(chunkStep)
                .build();
    }

    @Bean
    public Step chunkStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                          CursorUserReader reader,
                          UppercaseUsernameProcessor processor,
                          UserUpdateWriter writer) {
        return new StepBuilder("chunkStep", jobRepository)
                .<Map<String, Object>, Map<String, Object>>chunk(500, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public CursorUserReader cursorUserReader(JdbcTemplate jdbcTemplate) {
        CursorUserReader reader = new CursorUserReader();
        reader.setJdbcTemplate(jdbcTemplate);
        reader.setPageSize(500);
        return reader;
    }

    @Bean
    public UppercaseUsernameProcessor uppercaseUsernameProcessor() {
        return new UppercaseUsernameProcessor();
    }

    @Bean
    public UserUpdateWriter userUpdateWriter(JdbcTemplate jdbcTemplate) {
        UserUpdateWriter writer = new UserUpdateWriter();
        writer.setJdbcTemplate(jdbcTemplate);
        return writer;
    }
}
