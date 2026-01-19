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
}
