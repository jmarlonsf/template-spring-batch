package com.template.batch.config;

import com.template.batch.listener.BatchExecutionListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobConfig {

    /**
     * Job A - Processa dados da source_table_a
     * Job independente que executa apenas o stepJobA
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("jobA")
    public Job jobA(JobRepository jobRepository, Step stepJobA, BatchExecutionListener listener) {
        return new JobBuilder("jobA", jobRepository)
                .incrementer(new org.springframework.batch.core.launch.support.RunIdIncrementer())
                .listener(listener)
                .start(stepJobA)
                .build();
    }

    /**
     * Job B - Processa dados da source_table_b
     * Job independente que executa apenas o stepJobB
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("jobB")
    public Job jobB(JobRepository jobRepository, Step stepJobB, BatchExecutionListener listener) {
        return new JobBuilder("jobB", jobRepository)
                .incrementer(new org.springframework.batch.core.launch.support.RunIdIncrementer())
                .listener(listener)
                .start(stepJobB)
                .build();
    }
}
