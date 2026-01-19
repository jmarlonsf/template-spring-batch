package com.template.batch.config;

import com.template.batch.domain.SourceRecord;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.batch.core.configuration.annotation.StepScope;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

@Configuration
public class ReaderConfig {

    /**
     * RowMapper para converter ResultSet em SourceRecord
     */
    private RowMapper<SourceRecord> sourceRecordRowMapper() {
        return new RowMapper<SourceRecord>() {
            @Override
            public SourceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
                SourceRecord record = new SourceRecord();
                record.setId(rs.getLong("id"));
                record.setNome(rs.getString("nome"));
                record.setValor(rs.getBigDecimal("valor"));
                return record;
            }
        };
    }

    /**
     * Reader para source_table_a
     * Lê todos os registros da tabela source_table_a e retorna SourceRecord
     */
    @Bean
    @StepScope
    @Qualifier("sourceTableAReader")
    public JdbcCursorItemReader<SourceRecord> sourceTableAReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("sourceTableAReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM source_table_a")
                .rowMapper(sourceRecordRowMapper())
                .build();
    }

    /**
     * Reader para source_table_b
     * Lê todos os registros da tabela source_table_b e retorna SourceRecord
     */
    @Bean
    @StepScope
    @Qualifier("sourceTableBReader")
    public JdbcCursorItemReader<SourceRecord> sourceTableBReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("sourceTableBReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM source_table_b")
                .rowMapper(sourceRecordRowMapper())
                .build();
    }
}
