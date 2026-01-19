package com.template.batch.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TargetRecord {
    private Long id;
    private String nome;
    private BigDecimal valor;
    private LocalDateTime processadoEm;

    public TargetRecord() {
    }

    public TargetRecord(Long id, String nome, BigDecimal valor, LocalDateTime processadoEm) {
        this.id = id;
        this.nome = nome;
        this.valor = valor;
        this.processadoEm = processadoEm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDateTime getProcessadoEm() {
        return processadoEm;
    }

    public void setProcessadoEm(LocalDateTime processadoEm) {
        this.processadoEm = processadoEm;
    }
}
