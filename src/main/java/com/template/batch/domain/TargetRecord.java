package com.template.batch.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TargetRecord {
    private Long id;
    private String nome;
    private BigDecimal valor;
    private LocalDateTime processadoEm;
}
