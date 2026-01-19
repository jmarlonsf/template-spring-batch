package com.template.batch.config;

import com.template.batch.domain.MergedRecord;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ItemReader customizado que faz merge entre staging_table_a e staging_table_b
 * Implementa ItemStream para gerenciar ciclo de vida do reader interno
 */
public class MergedRecordItemReader extends ItemStreamSupport implements ItemReader<MergedRecord> {

    private final JdbcCursorItemReader<StagingRecordA> stagingAReader;
    private final JdbcTemplate jdbcTemplate;

    // Classe para representar registro de staging_table_a
    static class StagingRecordA {
        private Long id;
        private String name;
        private BigDecimal value;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
    }

    public MergedRecordItemReader(JdbcCursorItemReader<StagingRecordA> stagingAReader, JdbcTemplate jdbcTemplate) {
        this.stagingAReader = stagingAReader;
        this.jdbcTemplate = jdbcTemplate;
        // Define nome para logging
        setName("mergedRecordReader");
    }

    @Override
    public MergedRecord read() throws Exception {
        // Lê próximo registro de staging_table_a
        StagingRecordA recordA = stagingAReader.read();
        
        if (recordA == null) {
            // Não há mais dados
            return null;
        }

        // Faz lookup em staging_table_b usando JdbcTemplate
        // Usa queryForObject com tratamento de exceção para LEFT JOIN
        MergedRecord merged = null;
        try {
            merged = jdbcTemplate.queryForObject(
                    "SELECT name, value FROM staging_table_b WHERE id = ?",
                    new RowMapper<MergedRecord>() {
                        @Override
                        public MergedRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
                            MergedRecord m = new MergedRecord();
                            m.setId(recordA.getId());
                            m.setNameA(recordA.getName());
                            m.setValueA(recordA.getValue());
                            m.setNameB(rs.getString("name"));
                            m.setValueB(rs.getBigDecimal("value"));
                            return m;
                        }
                    },
                    recordA.getId()
            );
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // Não encontrou correspondente em staging_table_b (LEFT JOIN)
            merged = new MergedRecord();
            merged.setId(recordA.getId());
            merged.setNameA(recordA.getName());
            merged.setValueA(recordA.getValue());
            merged.setNameB(null);
            merged.setValueB(null);
        }

        return merged;
    }

    @Override
    public void open(org.springframework.batch.item.ExecutionContext executionContext) {
        super.open(executionContext);
        // IMPORTANTE: Abre o reader interno ANTES de poder ler
        // Spring Batch chama este método automaticamente quando detecta ItemStream
        if (stagingAReader instanceof ItemStream) {
            ((ItemStream) stagingAReader).open(executionContext);
        }
    }

    @Override
    public void update(org.springframework.batch.item.ExecutionContext executionContext) {
        super.update(executionContext);
        // Delega atualização para o reader interno (salva estado do cursor)
        // Chamado periodicamente pelo Spring Batch para salvar progresso
        if (stagingAReader instanceof ItemStream) {
            ((ItemStream) stagingAReader).update(executionContext);
        }
    }

    @Override
    public void close() {
        super.close();
        // Delega fechamento para o reader interno
        // Chamado quando o step finaliza (sucesso ou falha)
        if (stagingAReader instanceof ItemStream) {
            ((ItemStream) stagingAReader).close();
        }
    }
}
