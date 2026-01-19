package com.template.batch.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SourceRecord {
    private Long id;
    private String nome;
    private BigDecimal valor;
}
