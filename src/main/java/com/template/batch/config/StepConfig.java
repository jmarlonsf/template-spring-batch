package com.template.batch.config;

import com.template.batch.domain.SourceRecord;
import com.template.batch.domain.TargetRecord;
import com.template.batch.listener.BatchExecutionListener;
import com.template.batch.processor.CommonItemProcessor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class StepConfig {

    /**
     * Step para processar source_table_a
     * Lê da tabela A, processa e escreve na tabela de destino
     */
    @Bean
    public Step stepJobA(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("sourceTableAReader") JdbcCursorItemReader<SourceRecord> sourceTableAReader,
            CommonItemProcessor commonItemProcessor,
            JdbcBatchItemWriter<TargetRecord> targetTableWriter,
            BatchExecutionListener listener) {
        
        return new StepBuilder("stepJobA", jobRepository)
                .<SourceRecord, TargetRecord>chunk(10, transactionManager)
                .reader(sourceTableAReader)
                .processor(commonItemProcessor)
                .writer(targetTableWriter)
                .listener(listener)
                .build();
    }

    /**
     * Step para processar source_table_b
     * Lê da tabela B, processa e escreve na tabela de destino
     */
    @Bean
    public Step stepJobB(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("sourceTableBReader") JdbcCursorItemReader<SourceRecord> sourceTableBReader,
            CommonItemProcessor commonItemProcessor,
            JdbcBatchItemWriter<TargetRecord> targetTableWriter,
            BatchExecutionListener listener) {
        
        return new StepBuilder("stepJobB", jobRepository)
                .<SourceRecord, TargetRecord>chunk(10, transactionManager)
                .reader(sourceTableBReader)
                .processor(commonItemProcessor)
                .writer(targetTableWriter)
                .listener(listener)
                .build();
    }
}
