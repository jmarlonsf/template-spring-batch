package com.template.batch.processor;

import com.template.batch.domain.SourceRecord;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Processor pass-through (sem transformação)
 * 
 * USO:
 * - Para steps que apenas copiam dados sem transformação
 * - Exemplo: source_table_a → staging_table_a (apenas cópia)
 * 
 * POR QUE USAR PASS-THROUGH?
 * - Mantém a arquitetura consistente (Reader → Processor → Writer)
 * - Permite adicionar transformações depois sem quebrar o design
 * - Facilita adicionar validações ou logging no futuro
 * - Spring Batch requer um Processor (mesmo que seja pass-through)
 */
@Component
public class PassThroughProcessor implements ItemProcessor<SourceRecord, SourceRecord> {

    @Override
    public SourceRecord process(SourceRecord item) throws Exception {
        // Pass-through: retorna o item sem transformação
        // A transformação de campos (nome → name, valor → value) é feita no Writer
        return item;
    }
}
