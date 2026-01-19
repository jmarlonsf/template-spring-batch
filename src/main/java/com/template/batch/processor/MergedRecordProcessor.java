package com.template.batch.processor;

import com.template.batch.domain.MergedRecord;
import com.template.batch.domain.TargetRecord;
import com.template.batch.util.DateParameterUtil;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Processor para transformar MergedRecord (resultado do merge) em TargetRecord
 * 
 * POR QUE AGORA O PROCESSOR FAZ SENTIDO?
 * 
 * 1. DADOS JÁ ADQUIRIDOS E COMBINADOS
 *    - Reader já fez o merge (aquisição de dados)
 *    - Processor recebe dados já combinados (MergedRecord)
 *    - Não precisa fazer lookup ou merge no processor
 *    - Foca apenas em transformação e regras de negócio
 * 
 * 2. REGRAS DE NEGÓCIO COMPLEXAS
 *    - Precisa decidir qual nome usar (nameA ou nameB?)
 *    - Precisa calcular valor (somar? média? maior?)
 *    - Precisa aplicar validações (valores nulos, limites, etc.)
 *    - Precisa gerar metadados (processed_at = now())
 *    - Essas são transformações de negócio, não aquisição
 * 
 * 3. SEPARAÇÃO DE RESPONSABILIDADES
 *    - Reader: aquisição e merge de dados (I/O)
 *    - Processor: transformação e regras de negócio (lógica)
 *    - Writer: persistência (I/O)
 *    - Cada componente tem responsabilidade clara
 * 
 * 4. DIFERENÇA DO PASS-THROUGH
 *    - PassThroughProcessor: apenas copia dados (sem transformação)
 *    - MergedRecordProcessor: aplica regras de negócio (com transformação)
 *    - Pass-through faz sentido quando não há transformação necessária
 *    - Processor faz sentido quando há lógica de negócio a aplicar
 * 
 * 5. FLEXIBILIDADE E MANUTENIBILIDADE
 *    - Regras de negócio podem mudar frequentemente
 *    - Fácil de modificar sem alterar Reader ou Writer
 *    - Pode adicionar validações, cálculos, transformações
 *    - Código de negócio isolado e testável
 * 
 * 6. TESTABILIDADE
 *    - Fácil testar regras de negócio isoladamente
 *    - Não depende de I/O ou banco de dados
 *    - Pode testar diferentes cenários (com/sem nameB, valores nulos, etc.)
 * 
 * COMPARAÇÃO:
 * 
 * ❌ PassThroughProcessor (loadStagingAStep/loadStagingBStep):
 *    - Apenas copia dados: SourceRecord → SourceRecord
 *    - Sem transformação necessária
 *    - Mapeamento de campos feito no Writer
 * 
 * ✅ MergedRecordProcessor (merge final):
 *    - Aplica regras de negócio: MergedRecord → TargetRecord
 *    - Decide qual nome usar
 *    - Calcula valor (soma, média, etc.)
 *    - Gera metadados (processed_at)
 *    - Aplica validações
 * 
 * REGRAS DE NEGÓCIO APLICADAS:
 * 
 * 1. Nome:
 *    - Prefere nameA se disponível
 *    - Senão usa nameB
 *    - Senão usa "Sem nome"
 * 
 * 2. Valor:
 *    - Soma valueA + valueB se ambos existirem
 *    - Senão usa valueA se disponível
 *    - Senão usa valueB se disponível
 *    - Senão usa 0
 * 
 * 3. ProcessadoEm:
 *    - Sempre LocalDateTime.now() (momento do processamento)
 * 
 * 4. Validações:
 *    - Verifica se id não é null
 *    - Trata valores nulos graciosamente
 */
@Component
@StepScope
public class MergedRecordProcessor implements ItemProcessor<MergedRecord, TargetRecord> {

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
    public TargetRecord process(MergedRecord merged) throws Exception {
        TargetRecord target = new TargetRecord();
        
        // REGRA DE NEGÓCIO 1: ID
        // Copia o ID diretamente
        target.setId(merged.getId());
        
        // REGRA DE NEGÓCIO 2: NOME
        // Prefere nameA, senão nameB, senão valor padrão
        String nome = null;
        if (merged.getNameA() != null && !merged.getNameA().trim().isEmpty()) {
            nome = merged.getNameA();
        } else if (merged.getNameB() != null && !merged.getNameB().trim().isEmpty()) {
            nome = merged.getNameB();
        } else {
            nome = "Sem nome"; // Valor padrão
        }
        target.setNome(nome);
        
        // REGRA DE NEGÓCIO 3: VALOR
        // Soma valueA + valueB se ambos existirem
        // Senão usa o que estiver disponível
        BigDecimal valor = BigDecimal.ZERO;
        if (merged.getValueA() != null && merged.getValueB() != null) {
            // Ambos existem: soma
            valor = merged.getValueA().add(merged.getValueB());
        } else if (merged.getValueA() != null) {
            // Apenas valueA existe
            valor = merged.getValueA();
        } else if (merged.getValueB() != null) {
            // Apenas valueB existe
            valor = merged.getValueB();
        }
        // Se ambos forem null, valor permanece ZERO
        target.setValor(valor);
        
        // REGRA DE NEGÓCIO 4: PROCESSADO_EM
        // Preenche com timestamp do parâmetro 'processDate' se fornecido (formato yyyyMMdd),
        // senão usa timestamp atual (momento do processamento)
        // Não pode ser feito no SQL (depende do momento da execução ou parâmetro)
        LocalDateTime processDate = DateParameterUtil.getProcessDateOrDefault(jobParameters);
        target.setProcessadoEm(processDate);
        
        // VALIDAÇÃO: Verifica se ID não é null
        if (target.getId() == null) {
            throw new IllegalArgumentException("ID não pode ser null no MergedRecord");
        }
        
        return target;
    }
}
