package com.template.batch.processor;

import com.template.batch.domain.SourceRecord;
import com.template.batch.domain.TargetRecord;
import com.template.batch.util.DateParameterUtil;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Processor para transformar SourceRecord em TargetRecord
 * 
 * QUANDO FAZER TRANSFORMAÇÕES AQUI vs NO SQL:
 * 
 * ✅ FAÇA NO PROCESSOR quando:
 * - Lógica de negócio complexa (múltiplas condições, regras de negócio)
 * - Validações que dependem de múltiplos campos
 * - Chamadas a serviços externos (APIs, cache, outros sistemas)
 * - Transformações que dependem de estado/contexto da aplicação
 * - Regras que mudam frequentemente (mais fácil de manter em Java)
 * - Transformações que não são suportadas pelo banco
 * - Dados dinâmicos (ex: LocalDateTime.now(), UUID.randomUUID())
 * 
 * ❌ NÃO FAÇA NO PROCESSOR quando:
 * - Filtros simples (WHERE) - SEMPRE no SQL!
 * - Transformações de string simples (UPPER, LOWER, TRIM) - faça no SQL
 * - Cálculos matemáticos simples - faça no SQL
 * - Joins - SEMPRE no SQL!
 * - Agregações (COUNT, SUM, AVG) - SEMPRE no SQL!
 * 
 * EXEMPLOS:
 * 
 * ❌ ERRADO (fazer no SQL):
 * - Filtrar registros com valor > 100
 * - Fazer UPPER(nome) 
 * - Calcular valor * 1.1
 * 
 * ✅ CERTO (fazer no Processor):
 * - Validar regra: "se valor > 1000 E nome contém 'VIP', aplicar desconto especial"
 * - Buscar categoria em cache/serviço externo
 * - Gerar timestamp de processamento (LocalDateTime.now())
 * - Aplicar lógica condicional complexa
 */
@Component
@StepScope
public class CommonItemProcessor implements ItemProcessor<SourceRecord, TargetRecord> {

    private JobParameters jobParameters;

    /**
     * Injeção via setter para evitar problemas de conversão do Spring
     * StepExecution é automaticamente disponibilizado no contexto do step
     */
    @Value("#{stepExecution}")
    public void setStepExecution(StepExecution stepExecution) {
        if (stepExecution != null) {
            this.jobParameters = stepExecution.getJobParameters();
        }
    }

    @Override
    public TargetRecord process(SourceRecord source) throws Exception {
        TargetRecord target = new TargetRecord();
        
        // Copia os campos do SourceRecord para TargetRecord
        // NOTA: Se essas transformações fossem simples (ex: UPPER, TRIM),
        // seria melhor fazer direto no SQL do Reader
        target.setId(source.getId());
        target.setNome(source.getNome());
        target.setValor(source.getValor());
        
        // EXEMPLO: Transformação que DEVE ser feita no Processor
        // - Dados dinâmicos (timestamp atual ou do parâmetro)
        // - Não pode ser feito no SQL (depende do momento da execução ou parâmetro)
        // - Usa parâmetro 'processDate' se fornecido (formato yyyyMMdd), senão usa LocalDateTime.now()
        LocalDateTime processDate = DateParameterUtil.getProcessDateOrDefault(jobParameters);
        target.setProcessadoEm(processDate);
        
        // EXEMPLO: Lógica de negócio complexa (deveria estar aqui)
        // if (source.getValor().compareTo(new BigDecimal("1000")) > 0 && 
        //     source.getNome().contains("VIP")) {
        //     // Aplicar desconto especial
        //     target.setValor(source.getValor().multiply(new BigDecimal("0.9")));
        // }
        
        return target;
    }
}
