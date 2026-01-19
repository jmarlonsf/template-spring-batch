package com.template.batch.domain;

import java.math.BigDecimal;

/**
 * DTO para representar o resultado do merge entre staging_table_a e staging_table_b
 * 
 * ESTRUTURA:
 * - id: Chave comum (chave de junção)
 * - nameA: Nome da staging_table_a
 * - valueA: Valor da staging_table_a
 * - nameB: Nome da staging_table_b (pode ser null se não houver correspondente)
 * - valueB: Valor da staging_table_b (pode ser null se não houver correspondente)
 */
public class MergedRecord {
    
    private Long id;
    private String nameA;
    private BigDecimal valueA;
    private String nameB;
    private BigDecimal valueB;

    public MergedRecord() {
    }

    public MergedRecord(Long id, String nameA, BigDecimal valueA, String nameB, BigDecimal valueB) {
        this.id = id;
        this.nameA = nameA;
        this.valueA = valueA;
        this.nameB = nameB;
        this.valueB = valueB;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNameA() {
        return nameA;
    }

    public void setNameA(String nameA) {
        this.nameA = nameA;
    }

    public BigDecimal getValueA() {
        return valueA;
    }

    public void setValueA(BigDecimal valueA) {
        this.valueA = valueA;
    }

    public String getNameB() {
        return nameB;
    }

    public void setNameB(String nameB) {
        this.nameB = nameB;
    }

    public BigDecimal getValueB() {
        return valueB;
    }

    public void setValueB(BigDecimal valueB) {
        this.valueB = valueB;
    }

    @Override
    public String toString() {
        return "MergedRecord{" +
                "id=" + id +
                ", nameA='" + nameA + '\'' +
                ", valueA=" + valueA +
                ", nameB='" + nameB + '\'' +
                ", valueB=" + valueB +
                '}';
    }
}
