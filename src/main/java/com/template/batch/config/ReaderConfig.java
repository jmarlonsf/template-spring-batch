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
     * RowMapper alternativo que já aplica transformações no SQL
     * Exemplo: se você fizer SELECT UPPER(nome) as nome_upper, pode mapear direto
     */
    private RowMapper<SourceRecord> sourceRecordRowMapperWithTransformations() {
        return new RowMapper<SourceRecord>() {
            @Override
            public SourceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
                SourceRecord record = new SourceRecord();
                record.setId(rs.getLong("id"));
                // Exemplo: se SQL já retornar nome transformado, usa direto
                record.setNome(rs.getString("nome")); // ou "nome_upper" se usar alias
                record.setValor(rs.getBigDecimal("valor"));
                return record;
            }
        };
    }

    /**
     * Reader para source_table_a usando JdbcCursorItemReader
     * 
     * REGRA DE OURO: Onde fazer transformações?
     * 
     * ✅ FAÇA NO SQL (query) quando:
     * - Transformação é simples (UPPER, LOWER, TRIM, CONCAT, etc.)
     * - Filtros (WHERE) - SEMPRE faça no SQL!
     * - Joins e agregações
     * - Cálculos matemáticos simples
     * - Formatação de datas (TO_CHAR, DATE_FORMAT)
     * - Conversões de tipo (CAST, ::type)
     * - Performance crítica (milhões de registros)
     * 
     * ✅ FAÇA NO PROCESSOR (Java) quando:
     * - Lógica de negócio complexa
     * - Validações que requerem múltiplos campos
     * - Chamadas a serviços externos (APIs, outros sistemas)
     * - Transformações que dependem de estado/contexto
     * - Regras que podem mudar frequentemente
     * - Transformações que não são suportadas pelo banco
     * 
     * EXEMPLOS PRÁTICOS:
     * 
     * 1. FILTRO - SEMPRE NO SQL:
     *    ✅ .sql("SELECT * FROM source_table_a WHERE valor > 100")
     *    ❌ Trazer tudo e filtrar no Processor
     * 
     * 2. TRANSFORMAÇÃO SIMPLES - PREFIRA SQL:
     *    ✅ .sql("SELECT id, UPPER(nome) as nome, valor FROM source_table_a")
     *    ❌ SELECT nome e fazer .toUpperCase() no Processor
     * 
     * 3. JOIN - SEMPRE NO SQL:
     *    ✅ .sql("SELECT s.*, c.categoria FROM source_table_a s JOIN categorias c ON s.id = c.id")
     *    ❌ Buscar dados separados e fazer join no Processor
     * 
     * 4. LÓGICA COMPLEXA - PROCESSOR:
     *    ✅ Processor: if (valor > 100 && nome.contains("VIP")) { ... }
     *    ❌ Tentar fazer no SQL com CASE WHEN muito complexo
     * 
     * PERFORMANCE:
     * - SQL: Banco otimiza, usa índices, processa em lote
     * - Processor: Processa item por item, mais lento para grandes volumes
     * 
     * MEMÓRIA:
     * - SQL com WHERE: Reduz dados transferidos (menos I/O)
     * - Processor: Traz tudo e filtra depois (mais I/O, mais memória)
     */
    @Bean
    @StepScope
    @Qualifier("sourceTableAReader")
    public JdbcCursorItemReader<SourceRecord> sourceTableAReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("sourceTableAReader")
                .dataSource(dataSource)
                // EXEMPLO: Query otimizada com filtros e transformações no SQL
                // .sql("SELECT id, UPPER(TRIM(nome)) as nome, valor * 1.1 as valor " +
                //      "FROM source_table_a " +
                //      "WHERE valor > 100 AND nome IS NOT NULL " +
                //      "ORDER BY id")
                .sql("SELECT * FROM source_table_a")
                .rowMapper(sourceRecordRowMapper())
                // Opcional: controla quantos registros buscar por vez do cursor
                // .fetchSize(100) // PostgreSQL padrão é geralmente 50-100
                // Opcional: valida posição do cursor (recomendado manter true)
                // .verifyCursorPosition(true)
                .build();
    }

    /**
     * Reader para source_table_b usando JdbcCursorItemReader
     * 
     * Mesmo comportamento de streaming que sourceTableAReader
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
