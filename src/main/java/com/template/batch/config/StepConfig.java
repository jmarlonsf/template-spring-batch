package com.template.batch.config;

import com.template.batch.config.MergedRecordItemReader;
import com.template.batch.domain.JoinedSourceRecord;
import com.template.batch.domain.MergedRecord;
import com.template.batch.domain.SourceRecord;
import com.template.batch.domain.TargetRecord;
import com.template.batch.listener.BatchExecutionListener;
import com.template.batch.processor.CommonItemProcessor;
import com.template.batch.processor.JoinedSourceRecordProcessor;
import com.template.batch.processor.MergedRecordProcessor;
import com.template.batch.processor.PassThroughProcessor;
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

    /**
     * Step para processar JOIN direto entre source_table_a e source_table_b
     * 
     * CARACTERÍSTICAS:
     * - JOIN é feito no SQL (Reader) - otimizado pelo banco
     * - Processor apenas mapeia campos (JoinedSourceRecord → TargetRecord)
     * - Writer persiste na target_table
     * 
     * DIFERENÇA DOS OUTROS STEPS:
     * - stepJobA/stepJobB: leem tabelas separadas (SourceRecord)
     * - joinDirectStep: lê resultado de JOIN (JoinedSourceRecord)
     */
    @Bean
    public Step joinDirectStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("joinedReader") JdbcCursorItemReader<JoinedSourceRecord> joinedReader,
            JoinedSourceRecordProcessor joinedSourceRecordProcessor,
            JdbcBatchItemWriter<TargetRecord> targetTableWriter,
            BatchExecutionListener listener) {
        
        return new StepBuilder("joinDirectStep", jobRepository)
                .<JoinedSourceRecord, TargetRecord>chunk(10, transactionManager)
                .reader(joinedReader)
                .processor(joinedSourceRecordProcessor)
                .writer(targetTableWriter)
                .listener(listener)
                .build();
    }

    /**
     * Step para carregar staging_table_a
     * 
     * FLUXO:
     * - Reader: lê de source_table_a (readerSourceA)
     * - Processor: pass-through (sem transformação)
     * - Writer: escreve em staging_table_a (writerStagingA)
     * 
     * POR QUE SEPARAR OS STEPS?
     * 
     * 1. INDEPENDÊNCIA E PARALELIZAÇÃO
     *    - loadStagingAStep e loadStagingBStep são independentes
     *    - Podem ser executados em paralelo (TaskExecutor)
     *    - Não há dependência entre eles
     *    - Melhor aproveitamento de recursos (CPU, I/O)
     * 
     * 2. REPROCESSAMENTO SELETIVO
     *    - Pode reprocessar apenas loadStagingAStep se necessário
     *    - Pode reprocessar apenas loadStagingBStep se necessário
     *    - Não precisa reprocessar ambos se apenas um falhar
     *    - Economiza tempo e recursos
     * 
     * 3. MONITORAMENTO E DEBUG
     *    - Pode monitorar cada step separadamente
     *    - Logs mais claros (sabemos qual step está executando)
     *    - Facilita debug (identificar qual step tem problema)
     *    - Métricas por step (tempo, registros processados)
     * 
     * 4. RESTARTABILITY
     *    - Spring Batch gerencia estado de cada step separadamente
     *    - Se loadStagingAStep falhar, loadStagingBStep pode ter sucesso
     *    - Pode reiniciar apenas o step que falhou
     *    - Estado persistido por step
     * 
     * 5. FLEXIBILIDADE
     *    - Pode adicionar validações específicas por step
     *    - Pode adicionar listeners específicos por step
     *    - Pode configurar chunk size diferente por step
     *    - Pode adicionar skip/retry policies específicas
     * 
     * 6. MANUTENIBILIDADE
     *    - Código mais organizado e claro
     *    - Fácil de entender o fluxo
     *    - Fácil de modificar um step sem afetar o outro
     *    - Separação de responsabilidades
     * 
     * CENÁRIO SEM SEPARAÇÃO:
     *    Step único: source_table_a + source_table_b → staging
     *    - Se falhar, precisa reprocessar tudo
     *    - Não pode paralelizar
     *    - Difícil identificar qual tabela tem problema
     * 
     * CENÁRIO COM SEPARAÇÃO:
     *    loadStagingAStep: source_table_a → staging_table_a
     *    loadStagingBStep: source_table_b → staging_table_b
     *    - Se loadStagingAStep falhar, loadStagingBStep pode continuar
     *    - Pode executar em paralelo
     *    - Fácil identificar qual step tem problema
     */
    @Bean
    public Step loadStagingAStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("readerSourceA") JdbcCursorItemReader<SourceRecord> readerSourceA,
            PassThroughProcessor passThroughProcessor,
            @Qualifier("writerStagingA") JdbcBatchItemWriter<SourceRecord> writerStagingA,
            BatchExecutionListener listener) {
        
        return new StepBuilder("loadStagingAStep", jobRepository)
                .<SourceRecord, SourceRecord>chunk(10, transactionManager)
                .reader(readerSourceA)
                .processor(passThroughProcessor)
                .writer(writerStagingA)
                .listener(listener)
                .build();
    }

    /**
     * Step para carregar staging_table_b
     * 
     * POR QUE SEPARAR OS STEPS?
     * 
     * Mesmas razões do loadStagingAStep:
     * 
     * 1. INDEPENDÊNCIA E PARALELIZAÇÃO
     *    - Independente do loadStagingAStep
     *    - Pode executar em paralelo
     *    - Melhor aproveitamento de recursos
     * 
     * 2. REPROCESSAMENTO SELETIVO
     *    - Pode reprocessar apenas este step
     *    - Não precisa reprocessar loadStagingAStep
     * 
     * 3. MONITORAMENTO E DEBUG
     *    - Monitoramento separado
     *    - Logs claros
     *    - Fácil identificar problemas
     * 
     * 4. RESTARTABILITY
     *    - Estado gerenciado separadamente
     *    - Pode reiniciar apenas este step
     * 
     * 5. FLEXIBILIDADE
     *    - Configurações específicas
     *    - Validações específicas
     * 
     * 6. MANUTENIBILIDADE
     *    - Código organizado
     *    - Fácil de modificar
     */
    @Bean
    public Step loadStagingBStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("readerSourceB") JdbcCursorItemReader<SourceRecord> readerSourceB,
            PassThroughProcessor passThroughProcessor,
            @Qualifier("writerStagingB") JdbcBatchItemWriter<SourceRecord> writerStagingB,
            BatchExecutionListener listener) {
        
        return new StepBuilder("loadStagingBStep", jobRepository)
                .<SourceRecord, SourceRecord>chunk(10, transactionManager)
                .reader(readerSourceB)
                .processor(passThroughProcessor)
                .writer(writerStagingB)
                .listener(listener)
                .build();
    }

    /**
     * Step final responsável pelo merge entre staging_table_a e staging_table_b
     * 
     * FLUXO DO MERGE:
     * 
     * 1. READER (mergedRecordReader):
     *    - Lê registros de staging_table_a (streaming via cursor)
     *    - Para cada registro, faz lookup em staging_table_b usando JdbcTemplate
     *    - Combina dados em MergedRecord (nameA, valueA, nameB, valueB)
     *    - Retorna MergedRecord ou null se não houver mais dados
     * 
     * 2. PROCESSOR (mergedRecordProcessor):
     *    - Recebe MergedRecord (dados já combinados)
     *    - Aplica regras de negócio:
     *      * Nome: prefere nameA, senão nameB, senão "Sem nome"
     *      * Valor: soma valueA + valueB se ambos existirem
     *      * ProcessadoEm: LocalDateTime.now()
     *    - Valida dados (ID não null, valores nulos, etc.)
     *    - Retorna TargetRecord
     * 
     * 3. WRITER (targetTableWriter):
     *    - Recebe TargetRecord
     *    - Persiste na target_table usando UPSERT (ON CONFLICT DO UPDATE)
     *    - Garante idempotência (pode executar múltiplas vezes)
     * 
     * FLUXO COMPLETO:
     * 
     * ┌─────────────────┐
     * │ staging_table_a │
     * └────────┬────────┘
     *          │
     *          │ Reader: lê registro
     *          │         + lookup em staging_table_b
     *          ▼
     * ┌─────────────────┐
     * │ MergedRecord    │ (id, nameA, valueA, nameB, valueB)
     * └────────┬────────┘
     *          │
     *          │ Processor: aplica regras de negócio
     *          │            - Decide nome
     *          │            - Calcula valor
     *          │            - Gera processed_at
     *          ▼
     * ┌─────────────────┐
     * │ TargetRecord    │ (id, nome, valor, processedo_em)
     * └────────┬────────┘
     *          │
     *          │ Writer: persiste com UPSERT
     *          ▼
     * ┌─────────────────┐
     * │ target_table    │
     * └─────────────────┘
     * 
     * CARACTERÍSTICAS:
     * 
     * 1. STREAMING:
     *    - Reader usa cursor (não carrega tudo na memória)
     *    - Processa registro por registro
     *    - Eficiente para grandes volumes
     * 
     * 2. LOOKUP OTIMIZADO:
     *    - JdbcTemplate com query preparada
     *    - Banco otimiza lookup (usa índices)
     *    - Lookup sob demanda (lazy loading)
     * 
     * 3. REGRAS DE NEGÓCIO:
     *    - Aplicadas no Processor (isoladas e testáveis)
     *    - Fácil de modificar sem alterar Reader/Writer
     *    - Validações incluídas
     * 
     * 4. IDEMPOTÊNCIA:
     *    - Writer usa UPSERT (ON CONFLICT DO UPDATE)
     *    - Pode executar múltiplas vezes sem duplicar dados
     *    - Facilita retry e reprocessamento
     * 
     * 5. RESTARTABILITY:
     *    - Spring Batch gerencia estado do cursor
     *    - Se falhar, pode reiniciar de onde parou
     *    - Lookup sempre busca dados atuais
     * 
     * EXEMPLO DE PROCESSAMENTO:
     * 
     * Registro 1:
     *   staging_table_a: id=1, name="Produto A1", value=100.50
     *   staging_table_b: id=1, name="Serviço B1", value=300.00
     *   
     *   Reader → MergedRecord(id=1, nameA="Produto A1", valueA=100.50, nameB="Serviço B1", valueB=300.00)
     *   Processor → TargetRecord(id=1, nome="Produto A1", valor=400.50, processedo_em=2026-01-19 10:30:00)
     *   Writer → INSERT/UPDATE na target_table
     * 
     * Registro 2 (sem correspondente em B):
     *   staging_table_a: id=2, name="Produto A2", value=250.75
     *   staging_table_b: (não encontrado)
     *   
     *   Reader → MergedRecord(id=2, nameA="Produto A2", valueA=250.75, nameB=null, valueB=null)
     *   Processor → TargetRecord(id=2, nome="Produto A2", valor=250.75, processedo_em=2026-01-19 10:30:01)
     *   Writer → INSERT/UPDATE na target_table
     */
    @Bean
    public Step mergeFinalStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            @Qualifier("mergedRecordReader") MergedRecordItemReader mergedRecordReader,
            MergedRecordProcessor mergedRecordProcessor,
            JdbcBatchItemWriter<TargetRecord> targetTableWriter,
            BatchExecutionListener listener) {
        
        // IMPORTANTE: Usar MergedRecordItemReader diretamente (não ItemReader)
        // Isso garante que Spring Batch detecte que implementa ItemStream
        // Spring Batch detecta automaticamente ItemStream e chama open() antes de read()
        return new StepBuilder("mergeFinalStep", jobRepository)
                .<MergedRecord, TargetRecord>chunk(10, transactionManager)
                .reader(mergedRecordReader)
                .processor(mergedRecordProcessor)
                .writer(targetTableWriter)
                .listener(listener)
                .build();
    }
}
