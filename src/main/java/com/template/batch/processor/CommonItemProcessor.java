package com.template.batch.processor;

import com.template.batch.domain.SourceRecord;
import com.template.batch.domain.TargetRecord;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CommonItemProcessor implements ItemProcessor<SourceRecord, TargetRecord> {

    @Override
    public TargetRecord process(SourceRecord source) throws Exception {
        TargetRecord target = new TargetRecord();
        
        // Copia os campos do SourceRecord para TargetRecord
        target.setId(source.getId());
        target.setNome(source.getNome());
        target.setValor(source.getValor());
        
        // Preenche a data/hora de processamento
        target.setProcessadoEm(LocalDateTime.now());
        
        return target;
    }
}
