package com.template.batch.config;

import com.template.batch.domain.TargetRecord;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import javax.sql.DataSource;

@Configuration
public class WriterConfig {

    /**
     * Writer para target_table usando ON CONFLICT (recomendado para Spring Batch)
     * 
     * VANTAGENS do ON CONFLICT:
     * - Mais simples e direto para operações item-por-item
     * - Otimizado especificamente para UPSERT no PostgreSQL
     * - Melhor performance para batches pequenos/médios
     * - Funciona desde PostgreSQL 9.5+
     * - Menos overhead de parsing e planejamento
     * 
     * DESVANTAGENS:
     * - Sintaxe específica do PostgreSQL (não é padrão SQL)
     * - Limitado a conflitos em constraints (PK, UNIQUE)
     */
    @Bean
    public JdbcBatchItemWriter<TargetRecord> targetTableWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<TargetRecord>()
                .dataSource(dataSource)
                .sql("INSERT INTO target_table (id, nome, valor, processedo_em) " +
                     "VALUES (:id, :nome, :valor, :processadoEm) " +
                     "ON CONFLICT (id) DO UPDATE SET " +
                     "nome = EXCLUDED.nome, " +
                     "valor = EXCLUDED.valor, " +
                     "processedo_em = EXCLUDED.processedo_em")
                .itemSqlParameterSourceProvider(item -> {
                    MapSqlParameterSource params = new MapSqlParameterSource();
                    params.addValue("id", item.getId());
                    params.addValue("nome", item.getNome());
                    params.addValue("valor", item.getValor());
                    params.addValue("processadoEm", item.getProcessadoEm());
                    return params;
                })
                .build();
    }

    /**
     * ALTERNATIVA: Writer usando MERGE INTO (PostgreSQL 15+)
     * 
     * NOTA: MERGE não funciona diretamente com JdbcBatchItemWriter porque:
     * - MERGE requer uma cláusula USING (tabela fonte)
     * - JdbcBatchItemWriter processa itens individuais, não tabelas
     * - Seria necessário criar uma tabela temporária ou usar um ItemWriter customizado
     * 
     * VANTAGENS do MERGE:
     * - Padrão SQL (portável entre bancos)
     * - Mais flexível (pode ter múltiplos WHEN MATCHED/NOT MATCHED)
     * - Melhor para operações bulk em tabelas grandes
     * - Pode incluir DELETE em alguns casos
     * 
     * DESVANTAGENS:
     * - Requer PostgreSQL 15+ (outubro 2022)
     * - Mais complexo para implementar com Spring Batch
     * - Pode ter overhead maior para batches pequenos
     * - Requer planejamento mais complexo pelo otimizador
     * 
     * PERFORMANCE:
     * - Para Spring Batch (chunks pequenos/médios): ON CONFLICT geralmente é mais rápido
     * - Para operações bulk massivas: MERGE pode ser mais eficiente
     * - Depende de índices, volume de dados e proporção INSERT vs UPDATE
     * 
     * RECOMENDAÇÃO: Manter ON CONFLICT para este caso de uso
     */
    /*
    @Bean
    public JdbcBatchItemWriter<TargetRecord> targetTableWriterWithMerge(DataSource dataSource) {
        // MERGE não é diretamente suportado pelo JdbcBatchItemWriter
        // Seria necessário um ItemWriter customizado que:
        // 1. Cria uma tabela temporária
        // 2. Insere os itens do chunk na tabela temporária
        // 3. Executa MERGE da tabela temporária para target_table
        // 4. Limpa a tabela temporária
        
        // Exemplo de SQL MERGE (não funciona diretamente com JdbcBatchItemWriter):
        // MERGE INTO target_table t
        // USING (VALUES (:id, :nome, :valor, :processadoEm)) AS s(id, nome, valor, processedo_em)
        // ON t.id = s.id
        // WHEN MATCHED THEN
        //   UPDATE SET nome = s.nome, valor = s.valor, processedo_em = s.processedo_em
        // WHEN NOT MATCHED THEN
        //   INSERT (id, nome, valor, processedo_em) VALUES (s.id, s.nome, s.valor, s.processedo_em);
        
        return null; // Implementação customizada necessária
    }
    */
}
