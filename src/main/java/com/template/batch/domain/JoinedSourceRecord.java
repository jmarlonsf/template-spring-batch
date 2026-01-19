package com.template.batch.domain;

import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) para representar o resultado de um JOIN SQL
 * entre source_table_a e source_table_b.
 * 
 * DECISÃO DE ARQUITETURA: Separação de Responsabilidades
 * 
 * Por que este DTO é separado dos modelos de origem (SourceRecord)?
 * 
 * 1. SEPARAÇÃO DE RESPONSABILIDADES (Single Responsibility Principle)
 *    - SourceRecord: representa uma única tabela (source_table_a OU source_table_b)
 *    - JoinedSourceRecord: representa o RESULTADO de um JOIN (dados de AMBAS as tabelas)
 *    - Cada classe tem uma responsabilidade única e clara
 * 
 * 2. INDEPENDÊNCIA DE ESTRUTURA DE DADOS
 *    - SourceRecord reflete a estrutura física da tabela (1 tabela = 1 classe)
 *    - JoinedSourceRecord reflete a estrutura lógica da QUERY (JOIN = dados combinados)
 *    - Mudanças nas tabelas não afetam diretamente o DTO de JOIN
 * 
 * 3. CLAREZA E MANUTENIBILIDADE
 *    - Código mais legível: JoinedSourceRecord deixa explícito que é resultado de JOIN
 *    - Facilita manutenção: desenvolvedor entende imediatamente a origem dos dados
 *    - Evita confusão: não mistura conceitos de "tabela única" vs "dados combinados"
 * 
 * 4. FLEXIBILIDADE DE QUERY
 *    - JOIN pode ter diferentes estruturas (LEFT, INNER, FULL OUTER)
 *    - Pode incluir campos calculados, aliases, transformações
 *    - DTO pode evoluir independentemente das tabelas originais
 * 
 * 5. PERFORMANCE E OTIMIZAÇÃO
 *    - JOIN é feito no banco (otimizado pelo SGBD)
 *    - DTO recebe apenas os dados necessários (não precisa fazer JOIN no Processor)
 *    - Reduz I/O e processamento em memória
 * 
 * 6. TESTABILIDADE
 *    - Fácil criar mocks/stubs do DTO para testes
 *    - Testes unitários não dependem da estrutura das tabelas originais
 *    - Isolamento de responsabilidades facilita testes
 * 
 * BOA PRÁTICA: Nunca fazer JOIN no ItemProcessor
 * - JOIN no SQL: banco otimiza, usa índices, processa em lote
 * - JOIN no Processor: lento, carrega dados desnecessários, não usa índices
 */
public class JoinedSourceRecord {
    
    /**
     * ID comum (chave de junção entre as tabelas)
     */
    private Long id;
    
    /**
     * Nome (pode vir de qualquer uma das tabelas ou ser um alias/transformação)
     */
    private String name;
    
    /**
     * Valor da tabela A (source_table_a.valor)
     */
    private BigDecimal valueA;
    
    /**
     * Valor da tabela B (source_table_b.valor)
     */
    private BigDecimal valueB;

    public JoinedSourceRecord() {
    }

    public JoinedSourceRecord(Long id, String name, BigDecimal valueA, BigDecimal valueB) {
        this.id = id;
        this.name = name;
        this.valueA = valueA;
        this.valueB = valueB;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getValueA() {
        return valueA;
    }

    public void setValueA(BigDecimal valueA) {
        this.valueA = valueA;
    }

    public BigDecimal getValueB() {
        return valueB;
    }

    public void setValueB(BigDecimal valueB) {
        this.valueB = valueB;
    }

    @Override
    public String toString() {
        return "JoinedSourceRecord{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", valueA=" + valueA +
                ", valueB=" + valueB +
                '}';
    }
}
