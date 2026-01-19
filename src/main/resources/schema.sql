-- Schema SQL para o banco PostgreSQL

-- ============================================================================
-- TABELAS DE ORIGEM (Source Tables)
-- ============================================================================

-- Tabela de origem A
CREATE TABLE IF NOT EXISTS source_table_a (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    valor DECIMAL(10, 2) NOT NULL
);

-- Tabela de origem B
CREATE TABLE IF NOT EXISTS source_table_b (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    valor DECIMAL(10, 2) NOT NULL
);

-- ============================================================================
-- TABELAS DE STAGING (Staging Tables)
-- ============================================================================

/**
 * PAPEL DAS TABELAS DE STAGING:
 * 
 * 1. ISOLAMENTO E CONTROLE
 *    - Separa dados de origem dos dados processados
 *    - Permite reprocessamento sem afetar dados de origem
 *    - Facilita rollback em caso de erro
 *    - Área intermediária para transformações
 * 
 * 2. FLUXO ETL EM ETAPAS
 *    - Step 1: source_table_a → staging_table_a (cópia/transformação inicial)
 *    - Step 2: source_table_b → staging_table_b (cópia/transformação inicial)
 *    - Step 3: staging_table_a + staging_table_b → target_table (merge final)
 *    - Cada step é independente e pode ser reprocessado isoladamente
 * 
 * 3. PERFORMANCE E OTIMIZAÇÃO
 *    - Staging pode ter índices otimizados para o merge
 *    - Permite limpeza/transformação antes do merge final
 *    - Reduz carga nas tabelas de origem durante processamento
 *    - Facilita paralelização (Step 1 e 2 podem rodar em paralelo)
 * 
 * 4. RESTARTABILITY E RECUPERAÇÃO
 *    - Se Step 3 falhar, dados já estão em staging (não precisa reprocessar Step 1 e 2)
 *    - Pode limpar staging e reprocessar apenas Step 3
 *    - Facilita debug: pode inspecionar dados em staging antes do merge
 * 
 * 5. VALIDAÇÃO E QUALIDADE
 *    - Pode validar dados em staging antes do merge
 *    - Pode aplicar regras de negócio específicas em cada staging
 *    - Facilita identificação de problemas (dados problemáticos ficam em staging)
 * 
 * 6. FLEXIBILIDADE
 *    - Pode adicionar campos calculados/transformados em staging
 *    - Pode fazer limpeza de dados (deduplicação, normalização)
 *    - Permite diferentes estratégias de merge (INNER, LEFT, FULL OUTER)
 * 
 * ESTRUTURA SIMPLES:
 * - id: Chave primária (mesma da origem para facilitar join)
 * - name: Nome do registro (pode ser transformado da origem)
 * - value: Valor do registro (pode ser transformado da origem)
 */

-- Tabela de staging A
-- Armazena dados processados de source_table_a
CREATE TABLE IF NOT EXISTS staging_table_a (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    value DECIMAL(10, 2) NOT NULL
);

-- Tabela de staging B
-- Armazena dados processados de source_table_b
CREATE TABLE IF NOT EXISTS staging_table_b (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    value DECIMAL(10, 2) NOT NULL
);

-- ============================================================================
-- TABELA DE DESTINO (Target Table)
-- ============================================================================

-- Tabela de destino
CREATE TABLE IF NOT EXISTS target_table (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    valor DECIMAL(10, 2) NOT NULL,
    processedo_em TIMESTAMP NOT NULL
);
