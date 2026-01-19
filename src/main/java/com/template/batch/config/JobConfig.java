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

    /**
     * Job para processar JOIN direto entre source_table_a e source_table_b
     * 
     * QUANDO USAR ESTE JOB?
     * 
     * 1. CENÁRIO: Dados relacionados que precisam ser combinados
     *    - Quando você precisa de dados de AMBAS as tabelas simultaneamente
     *    - Quando a lógica de negócio depende da relação entre tabelas
     *    - Exemplo: calcular valor total (valueA + valueB) para cada registro
     * 
     * 2. PERFORMANCE: Grande volume de dados
     *    - Quando há milhões de registros para processar
     *    - JOIN no SQL é 100-1000x mais rápido que no Processor
     *    - Reduz uso de memória (streaming vs carregar tudo)
     * 
     * 3. RESTARTABILITY: Processamento longo
     *    - Jobs que podem levar horas/dias
     *    - Precisa poder reiniciar de onde parou
     *    - Spring Batch gerencia estado automaticamente
     * 
     * 4. CONSISTÊNCIA: Snapshot transacional
     *    - Quando precisa de visão consistente dos dados
     *    - Cursor mantém snapshot durante toda a transação
     *    - Não há race conditions
     * 
     * 5. QUANDO NÃO USAR:
     *    - Dados independentes (use jobA e jobB separados)
     *    - Lógica simples que não precisa de JOIN
     *    - Volume pequeno onde performance não é crítica
     * 
     * EXEMPLOS DE USO:
     * 
     * ✅ USAR joinDirectJob quando:
     * - Calcular totais combinados (valueA + valueB)
     * - Validar consistência entre tabelas relacionadas
     * - Processar apenas registros que existem em ambas as tabelas
     * - ETL que precisa combinar dados de múltiplas fontes
     * - Relatórios que agregam dados relacionados
     * 
     * ❌ NÃO USAR joinDirectJob quando:
     * - Dados são independentes (processar separadamente)
     * - Não há relação entre as tabelas
     * - Volume pequeno (overhead do JOIN não compensa)
     * - Lógica pode ser feita em queries separadas
     * 
     * CONFIGURAÇÃO:
     * - Restartable: true (padrão) - permite reiniciar job interrompido
     * - Incrementer: RunIdIncrementer - permite executar múltiplas vezes
     * - Listener: BatchExecutionListener - logging e monitoramento
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("joinDirectJob")
    public Job joinDirectJob(JobRepository jobRepository, Step joinDirectStep, BatchExecutionListener listener) {
        return new JobBuilder("joinDirectJob", jobRepository)
                .incrementer(new org.springframework.batch.core.launch.support.RunIdIncrementer())
                .listener(listener)
                .start(joinDirectStep)
                .build();
    }

    /**
     * Job para processar JOIN posterior usando staging tables
     * 
     * FLUXO DO JOB:
     * 
     * Step 1: loadStagingAStep
     *   - Lê de source_table_a
     *   - Escreve em staging_table_a
     *   - Pass-through (sem transformação)
     * 
     * Step 2: loadStagingBStep
     *   - Lê de source_table_b
     *   - Escreve em staging_table_b
     *   - Pass-through (sem transformação)
     * 
     * Step 3: mergeFinalStep
     *   - Lê de staging_table_a (com lookup em staging_table_b)
     *   - Aplica regras de negócio (MergedRecord → TargetRecord)
     *   - Escreve em target_table
     * 
     * QUANDO USAR ESTE JOB?
     * 
     * 1. CENÁRIO: Processamento em etapas com staging
     *    - Quando precisa isolar dados de origem dos processados
     *    - Quando precisa reprocessar apenas etapas específicas
     *    - Quando precisa validar dados antes do merge final
     *    - Quando precisa de flexibilidade no reprocessamento
     * 
     * 2. REPROCESSAMENTO SELETIVO
     *    - Se Step 3 (merge) falhar, dados já estão em staging
     *    - Não precisa reprocessar Step 1 e Step 2
     *    - Apenas reprocessa Step 3 usando dados em staging
     *    - Economiza tempo e recursos
     * 
     * 3. VALIDAÇÃO E QUALIDADE
     *    - Pode inspecionar dados em staging antes do merge
     *    - Pode validar dados antes do Step 3
     *    - Facilita identificação de problemas
     *    - Dados intermediários ficam disponíveis para análise
     * 
     * 4. FLEXIBILIDADE
     *    - Pode reprocessar apenas uma staging (A ou B)
     *    - Pode reprocessar apenas o merge (Step 3)
     *    - Pode limpar staging e recomeçar
     *    - Diferentes estratégias de reprocessamento
     * 
     * 5. PARALELIZAÇÃO (FUTURA)
     *    - Step 1 e Step 2 podem ser executados em paralelo
     *    - Melhor aproveitamento de recursos
     *    - Reduz tempo total de execução
     * 
     * 6. QUANDO NÃO USAR:
     *    - Processamento simples que não precisa de staging
     *    - Volume pequeno onde overhead não compensa
     *    - Quando JOIN direto (joinDirectJob) é suficiente
     * 
     * COMPARAÇÃO COM OUTROS JOBS:
     * 
     * joinDirectJob:
     *   - JOIN direto no SQL (source_table_a + source_table_b → target_table)
     *   - Mais rápido para volumes grandes
     *   - Menos flexibilidade no reprocessamento
     *   - Não usa staging
     * 
     * joinStagingJob (este):
     *   - JOIN posterior via staging (source → staging → merge → target)
     *   - Mais flexível no reprocessamento
     *   - Permite validação intermediária
     *   - Usa staging tables
     * 
     * jobA + jobB:
     *   - Processa tabelas separadamente
     *   - Não faz merge
     *   - Dados independentes
     * 
     * EXEMPLOS DE USO:
     * 
     * ✅ USAR joinStagingJob quando:
     * - Precisa de reprocessamento seletivo (apenas merge, não staging)
     * - Precisa validar dados antes do merge final
     * - Precisa inspecionar dados intermediários
     * - Processamento longo que pode falhar no merge
     * - ETL complexo com múltiplas etapas
     * - Precisa de flexibilidade no reprocessamento
     * 
     * ❌ NÃO USAR joinStagingJob quando:
     * - Processamento simples (use joinDirectJob)
     * - Volume pequeno (overhead não compensa)
     * - Não precisa de staging (use joinDirectJob)
     * - Dados independentes (use jobA e jobB)
     * 
     * CONFIGURAÇÃO:
     * - Restartable: true (padrão) - permite reiniciar job interrompido
     * - Incrementer: RunIdIncrementer - permite executar múltiplas vezes
     * - Listener: BatchExecutionListener - logging e monitoramento
     * - Steps em sequência: loadStagingAStep → loadStagingBStep → mergeFinalStep
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("joinStagingJob")
    public Job joinStagingJob(
            JobRepository jobRepository, 
            Step loadStagingAStep,
            Step loadStagingBStep,
            Step mergeFinalStep,
            BatchExecutionListener listener) {
        return new JobBuilder("joinStagingJob", jobRepository)
                .incrementer(new org.springframework.batch.core.launch.support.RunIdIncrementer())
                .listener(listener)
                .start(loadStagingAStep)
                .next(loadStagingBStep)
                .next(mergeFinalStep)
                .build();
    }
}
