package com.template.batch.config;

import com.template.batch.domain.JoinedSourceRecord;
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

    /**
     * RowMapper para converter ResultSet em JoinedSourceRecord
     * Mapeia os campos do JOIN SQL para o DTO
     */
    private RowMapper<JoinedSourceRecord> joinedSourceRecordRowMapper() {
        return new RowMapper<JoinedSourceRecord>() {
            @Override
            public JoinedSourceRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
                JoinedSourceRecord record = new JoinedSourceRecord();
                record.setId(rs.getLong("id"));
                record.setName(rs.getString("name"));
                record.setValueA(rs.getBigDecimal("value_a"));
                record.setValueB(rs.getBigDecimal("value_b"));
                return record;
            }
        };
    }

    /**
     * Reader com JOIN SQL entre source_table_a e source_table_b
     * 
     * POR QUE O JOIN FICA NO READER (SQL) E NÃO NO PROCESSOR?
     * 
     * 1. PERFORMANCE - Diferença de 100x a 1000x mais rápido
     *    ✅ JOIN no SQL (Reader):
     *       - Banco otimiza o JOIN usando índices, estatísticas, planos de execução
     *       - Processa em lote, usa algoritmos otimizados (hash join, merge join, nested loop)
     *       - Reduz I/O: apenas dados necessários são transferidos
     *       - Exemplo: 1 milhão de registros = ~1-5 segundos
     *    
     *    ❌ JOIN no Processor:
     *       - Carrega TODAS as tabelas na memória (ou faz N queries)
     *       - Processa item por item (N x M comparações)
     *       - Não usa índices do banco
     *       - Exemplo: 1 milhão de registros = ~10-30 minutos
     * 
     * 2. MEMÓRIA - Reduz uso drasticamente
     *    ✅ JOIN no SQL:
     *       - Streaming: lê apenas o necessário do cursor
     *       - Memória: O(chunk_size) = ~10-100 registros
     *       - Processa milhões sem estourar memória
     *    
     *    ❌ JOIN no Processor:
     *       - Precisa carregar uma tabela inteira em memória (ou cache)
     *       - Memória: O(N) onde N = tamanho da tabela
     *       - Pode causar OutOfMemoryError com grandes volumes
     * 
     * 3. RESTARTABILITY - Funciona perfeitamente
     *    ✅ JOIN no SQL:
     *       - Cursor mantém posição através de restart
     *       - Spring Batch salva estado do cursor (BATCH_STEP_EXECUTION_CONTEXT)
     *       - Ao reiniciar, continua de onde parou
     *       - JOIN é reexecutado, mas cursor posiciona no registro correto
     *       - Funciona mesmo se dados mudarem entre execuções
     *    
     *    ⚠️ JOIN no Processor:
     *       - Estado do JOIN não é persistido facilmente
     *       - Precisa implementar lógica customizada de restart
     *       - Mais complexo e propenso a erros
     * 
     * 4. CONSISTÊNCIA TRANSACIONAL
     *    ✅ JOIN no SQL:
     *       - Snapshot consistente: vê dados no momento da transação
     *       - Cursor mantém visão isolada dos dados
     *       - Não há race conditions
     *    
     *    ❌ JOIN no Processor:
     *       - Dados podem mudar entre leituras
     *       - Precisa gerenciar transações manualmente
     *       - Possível inconsistência
     * 
     * 5. MANUTENIBILIDADE
     *    ✅ JOIN no SQL:
     *       - Query declarativa, fácil de entender
     *       - Pode ser testada diretamente no banco
     *       - Fácil de otimizar (EXPLAIN, índices)
     *    
     *    ❌ JOIN no Processor:
     *       - Lógica imperativa complexa
     *       - Difícil de testar e debugar
     *       - Difícil de otimizar
     * 
     * IMPACTO EM PERFORMANCE:
     * 
     * Cenário: JOIN entre 2 tabelas com 1 milhão de registros cada
     * 
     * JOIN no SQL (Reader):
     * - Tempo: ~2-5 segundos
     * - Memória: ~50-100 MB
     * - I/O: ~100-200 MB
     * - CPU: ~10-20% (banco otimiza)
     * 
     * JOIN no Processor:
     * - Tempo: ~10-30 minutos
     * - Memória: ~500 MB - 2 GB (depende do tamanho dos objetos)
     * - I/O: ~500 MB - 1 GB (múltiplas queries)
     * - CPU: ~80-100% (processamento em memória)
     * 
     * IMPACTO EM RESTARTABILITY:
     * 
     * ✅ JOIN no SQL:
     * - Spring Batch salva: job_execution_id, step_execution_id, commit_count, read_count
     * - Ao reiniciar: reexecuta query, mas posiciona cursor no registro correto
     * - Funciona mesmo se:
     *   * Dados mudaram entre execuções
     *   * Novos registros foram adicionados
     *   * Job foi interrompido no meio
     * 
     * ⚠️ JOIN no Processor:
     * - Precisa salvar estado manualmente (qual registro foi processado)
     * - Lógica de restart complexa
     * - Pode processar registros duplicados ou pular registros
     * 
     * DECISÃO ARQUITETURAL:
     * - JOIN SEMPRE no SQL (Reader)
     * - Processor apenas para lógica de negócio
     * - Segue princípio: "Push processing to the database"
     */
    @Bean
    @StepScope
    @Qualifier("joinedReader")
    public JdbcCursorItemReader<JoinedSourceRecord> joinedReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<JoinedSourceRecord>()
                .name("joinedReader")
                .dataSource(dataSource)
                // JOIN INNER: retorna apenas registros que existem em ambas as tabelas
                // Usa aliases para evitar ambiguidade de nomes de colunas
                .sql("SELECT " +
                     "    a.id, " +
                     "    COALESCE(a.nome, b.nome) as name, " +  // Prefere nome de A, senão de B
                     "    a.valor as value_a, " +
                     "    b.valor as value_b " +
                     "FROM source_table_a a " +
                     "INNER JOIN source_table_b b ON a.id = b.id " +
                     "ORDER BY a.id")  // ORDER BY importante para restartability consistente
                .rowMapper(joinedSourceRecordRowMapper())
                // Fetch size otimizado para JOINs (pode ser maior que leitura simples)
                .fetchSize(100)
                .build();
    }

    /**
     * Reader para source_table_a (fluxo de staging)
     * 
     * POR QUE ESTE READER CONTINUA SIMPLES?
     * 
     * 1. RESPONSABILIDADE ÚNICA
     *    - Apenas lê dados de uma única tabela (source_table_a)
     *    - Não faz JOINs, agregações ou transformações complexas
     *    - Query simples: SELECT * FROM source_table_a
     *    - Mapeamento direto: ResultSet → SourceRecord
     * 
     * 2. SEPARAÇÃO DE CONCERNS
     *    - Reader: apenas leitura (I/O)
     *    - Processor: transformação de dados (lógica de negócio)
     *    - Writer: persistência (I/O)
     *    - Cada componente tem uma responsabilidade clara
     * 
     * 3. PERFORMANCE OTIMIZADA
     *    - Query simples = plano de execução simples
     *    - Banco otimiza automaticamente (índices, estatísticas)
     *    - Streaming via cursor (não carrega tudo na memória)
     *    - Fetch size padrão do driver é suficiente
     * 
     * 4. MANUTENIBILIDADE
     *    - Código simples = fácil de entender
     *    - Fácil de testar (apenas leitura)
     *    - Fácil de modificar (adicionar filtros WHERE se necessário)
     *    - Não há lógica complexa para quebrar
     * 
     * 5. REUSABILIDADE
     *    - Pode ser usado em diferentes contextos
     *    - Não acoplado a regras de negócio específicas
     *    - Pode ser facilmente substituído ou estendido
     * 
     * 6. RESTARTABILITY NATIVA
     *    - Spring Batch gerencia estado do cursor automaticamente
     *    - Não precisa de lógica customizada
     *    - Funciona out-of-the-box
     * 
     * DIFERENÇA DO READER COM JOIN:
     * - Este reader: query simples, uma tabela, mapeamento direto
     * - Reader com JOIN: query complexa, múltiplas tabelas, mapeamento de DTO
     * 
     * QUANDO ADICIONAR COMPLEXIDADE:
     * - Filtros: adicionar WHERE no SQL (ainda simples)
     * - Transformações simples: adicionar no SQL (UPPER, TRIM, etc.)
     * - JOINs: criar reader separado (como joinedReader)
     * - Lógica complexa: adicionar no Processor
     */
    @Bean
    @StepScope
    @Qualifier("readerSourceA")
    public JdbcCursorItemReader<SourceRecord> readerSourceA(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("readerSourceA")
                .dataSource(dataSource)
                .sql("SELECT * FROM source_table_a ORDER BY id")
                .rowMapper(sourceRecordRowMapper())
                .build();
    }

    /**
     * Reader para source_table_b (fluxo de staging)
     * 
     * POR QUE ESTE READER CONTINUA SIMPLES?
     * 
     * Mesmas razões do readerSourceA:
     * 
     * 1. RESPONSABILIDADE ÚNICA
     *    - Apenas lê dados de uma única tabela (source_table_b)
     *    - Query simples: SELECT * FROM source_table_b
     *    - Mapeamento direto: ResultSet → SourceRecord
     * 
     * 2. SEPARAÇÃO DE CONCERNS
     *    - Reader: apenas leitura
     *    - Processor: transformação
     *    - Writer: persistência
     * 
     * 3. PERFORMANCE
     *    - Query simples = otimização automática
     *    - Streaming via cursor
     *    - Baixo uso de memória
     * 
     * 4. MANUTENIBILIDADE
     *    - Código simples e claro
     *    - Fácil de testar e modificar
     * 
     * 5. REUSABILIDADE
     *    - Genérico, não acoplado a regras específicas
     * 
     * 6. RESTARTABILITY
     *    - Gerenciado automaticamente pelo Spring Batch
     * 
     * NOTA: ORDER BY id garante ordem consistente para restartability
     */
    @Bean
    @StepScope
    @Qualifier("readerSourceB")
    public JdbcCursorItemReader<SourceRecord> readerSourceB(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<SourceRecord>()
                .name("readerSourceB")
                .dataSource(dataSource)
                .sql("SELECT * FROM source_table_b ORDER BY id")
                .rowMapper(sourceRecordRowMapper())
                .build();
    }
}
