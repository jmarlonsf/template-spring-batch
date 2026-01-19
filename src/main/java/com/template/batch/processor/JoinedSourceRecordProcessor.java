package com.template.batch.processor;

import com.template.batch.domain.JoinedSourceRecord;
import com.template.batch.domain.TargetRecord;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Processor para transformar JoinedSourceRecord (resultado de JOIN) em TargetRecord
 * 
 * POR QUE O PROCESSOR CONTINUA SIMPLES?
 * 
 * 1. SEPARAÇÃO DE RESPONSABILIDADES (Single Responsibility Principle)
 *    - Reader: faz JOIN no SQL (otimizado pelo banco)
 *    - Processor: apenas mapeia campos e adiciona metadados (timestamp)
 *    - Writer: persiste dados
 *    - Cada componente tem uma única responsabilidade clara
 * 
 * 2. JOIN JÁ FOI FEITO NO READER (SQL)
 *    - O trabalho pesado (JOIN) foi feito no banco de dados
 *    - Processor recebe dados já combinados e prontos
 *    - Não precisa fazer lógica complexa de junção
 *    - Apenas transformação simples: DTO → Domain
 * 
 * 3. MAPEAMENTO DIRETO DE CAMPOS
 *    - id → id (direto)
 *    - name → nome (direto)
 *    - valueA/valueB → valor (escolha simples: usar valueA, somar, ou média)
 *    - LocalDateTime.now() → processadoEm (metadado de processamento)
 *    - Não há lógica de negócio complexa aqui
 * 
 * 4. PRINCÍPIO: "PUSH PROCESSING TO THE DATABASE"
 *    - JOIN no SQL: banco otimiza, usa índices, processa em lote
 *    - Processor: apenas transformação estrutural simples
 *    - Se precisar de lógica complexa, adicionar depois sem quebrar o design
 * 
 * 5. MANUTENIBILIDADE
 *    - Código simples = fácil de entender e manter
 *    - Fácil de testar (apenas mapeamento)
 *    - Fácil de estender (adicionar validações/lógica depois se necessário)
 * 
 * 6. REUSABILIDADE
 *    - Processor genérico pode ser usado em diferentes contextos
 *    - Não acoplado a regras de negócio específicas
 *    - Pode ser facilmente substituído ou estendido
 * 
 * DECISÃO DE MAPEAMENTO:
 * - valueA + valueB → valor (soma dos valores)
 *   Alternativas possíveis:
 *   - Usar apenas valueA: target.setValor(source.getValueA())
 *   - Usar apenas valueB: target.setValor(source.getValueB())
 *   - Calcular média: (valueA + valueB) / 2
 *   - Usar o maior: max(valueA, valueB)
 * 
 * NOTA: Este processor é separado do CommonItemProcessor porque:
 * - CommonItemProcessor: SourceRecord → TargetRecord (usado por jobA e jobB)
 * - JoinedSourceRecordProcessor: JoinedSourceRecord → TargetRecord (usado por job com JOIN)
 * - Mantém compatibilidade: jobA e jobB continuam funcionando normalmente
 */
@Component
public class JoinedSourceRecordProcessor implements ItemProcessor<JoinedSourceRecord, TargetRecord> {

    @Override
    public TargetRecord process(JoinedSourceRecord source) throws Exception {
        TargetRecord target = new TargetRecord();
        
        // Mapeamento direto de campos
        target.setId(source.getId());
        target.setNome(source.getName());
        
        // DECISÃO: Soma valueA + valueB para o valor final
        // Alternativas comentadas abaixo se precisar mudar a lógica
        if (source.getValueA() != null && source.getValueB() != null) {
            target.setValor(source.getValueA().add(source.getValueB()));
        } else if (source.getValueA() != null) {
            target.setValor(source.getValueA());
        } else if (source.getValueB() != null) {
            target.setValor(source.getValueB());
        }
        // Alternativas:
        // - Usar apenas valueA: target.setValor(source.getValueA())
        // - Usar apenas valueB: target.setValor(source.getValueB())
        // - Calcular média: target.setValor(source.getValueA().add(source.getValueB()).divide(new BigDecimal("2")))
        // - Usar o maior: target.setValor(source.getValueA().max(source.getValueB()))
        
        // Preenche timestamp de processamento (metadado dinâmico)
        // Não pode ser feito no SQL (depende do momento da execução)
        target.setProcessadoEm(LocalDateTime.now());
        
        return target;
    }
}
