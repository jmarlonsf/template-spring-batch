package com.template.batch.config;

import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Configuração do ItemReader customizado para fazer merge entre staging_table_a e staging_table_b
 * 
 * POR QUE O READER É RESPONSÁVEL PELO MERGE?
 * 
 * 1. SEPARAÇÃO DE RESPONSABILIDADES (Single Responsibility Principle)
 *    - Reader: responsável por LEITURA e AQUISIÇÃO de dados
 *    - Merge é parte da aquisição (buscar dados de múltiplas fontes)
 *    - Processor: responsável por TRANSFORMAÇÃO de dados já adquiridos
 *    - Writer: responsável por PERSISTÊNCIA de dados
 *    - Cada componente tem uma responsabilidade clara
 * 
 * 2. PERFORMANCE E EFICIÊNCIA
 *    - Reader pode fazer lookup otimizado (JdbcTemplate com query preparada)
 *    - Banco otimiza queries individuais (usa índices)
 *    - Lookup é feito sob demanda (lazy loading)
 *    - Processor recebe dados já combinados (não precisa fazer lookup)
 * 
 * 3. RESTARTABILITY
 *    - Spring Batch gerencia estado do cursor principal (staging_table_a)
 *    - Lookup é feito a cada read(), então é sempre atualizado
 *    - Se job reiniciar, continua de onde parou no cursor principal
 *    - Lookup sempre busca dados atuais do banco
 * 
 * 4. CONSISTÊNCIA TRANSACIONAL
 *    - Reader está dentro da transação do Step
 *    - Lookup vê dados consistentes (mesma transação)
 *    - Não há race conditions
 *    - Snapshot consistente dos dados
 * 
 * 5. FLEXIBILIDADE
 *    - Pode fazer LEFT JOIN (retorna null se não houver correspondente)
 *    - Pode fazer INNER JOIN (retorna apenas se houver correspondente)
 *    - Pode adicionar lógica de fallback no lookup
 *    - Fácil de modificar estratégia de merge
 * 
 * 6. TESTABILIDADE
 *    - Pode mockar JdbcTemplate para testes
 *    - Pode testar lookup isoladamente
 *    - Fácil de testar diferentes cenários (com/sem correspondente)
 * 
 * ALTERNATIVA (NÃO RECOMENDADA):
 * 
 * ❌ Fazer merge no Processor:
 *    - Processor receberia apenas dados de staging_table_a
 *    - Processor faria lookup em staging_table_b
 *    - Problemas:
 *      * Mistura responsabilidades (transformação + aquisição)
 *      * Lookup repetido para cada item (menos eficiente)
 *      * Difícil de testar
 *      * Não aproveita otimizações do banco
 * 
 * ✅ Fazer merge no Reader (atual):
 *    - Reader faz lookup durante a leitura
 *    - Processor recebe dados já combinados
 *    - Vantagens:
 *      * Separação clara de responsabilidades
 *      * Lookup otimizado pelo banco
 *      * Fácil de testar e manter
 *      * Aproveita otimizações do banco
 * 
 * IMPLEMENTAÇÃO:
 * - Usa JdbcCursorItemReader para ler staging_table_a (streaming)
 * - Usa JdbcTemplate para lookup em staging_table_b (query preparada)
 * - Combina dados em MergedRecord
 * - Retorna null quando não há mais dados
 */
@Configuration
public class MergedRecordReaderConfig {

    /**
     * Reader customizado que faz merge entre staging_table_a e staging_table_b
     * 
     * FLUXO:
     * 1. Lê registro de staging_table_a (streaming via cursor)
     * 2. Faz lookup em staging_table_b usando JdbcTemplate
     * 3. Combina dados em MergedRecord
     * 4. Retorna MergedRecord ou null se não houver mais dados
     */
    @Bean
    @StepScope
    @Qualifier("mergedRecordReader")
    public MergedRecordItemReader mergedRecordReader(DataSource dataSource) {
        // IMPORTANTE: Retorna MergedRecordItemReader diretamente (não ItemReader)
        // Isso garante que Spring Batch detecte que implementa ItemStream
        // Se retornar ItemReader, Spring pode criar proxy que não preserva ItemStream
        
        // Reader base para staging_table_a
        JdbcCursorItemReader<MergedRecordItemReader.StagingRecordA> stagingAReader = 
                new JdbcCursorItemReaderBuilder<MergedRecordItemReader.StagingRecordA>()
                .name("stagingAReader")
                .dataSource(dataSource)
                .sql("SELECT id, name, value FROM staging_table_a ORDER BY id")
                .rowMapper(new RowMapper<MergedRecordItemReader.StagingRecordA>() {
                    @Override
                    public MergedRecordItemReader.StagingRecordA mapRow(ResultSet rs, int rowNum) throws SQLException {
                        MergedRecordItemReader.StagingRecordA record = new MergedRecordItemReader.StagingRecordA();
                        record.setId(rs.getLong("id"));
                        record.setName(rs.getString("name"));
                        record.setValue(rs.getBigDecimal("value"));
                        return record;
                    }
                })
                .build();

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // Cria e retorna o reader customizado que implementa ItemStream
        // ItemStream é necessário para que Spring Batch gerencie o ciclo de vida (open, close, update)
        // Retornando MergedRecordItemReader diretamente, Spring Batch detecta automaticamente ItemStream
        return new MergedRecordItemReader(stagingAReader, jdbcTemplate);
    }
}
