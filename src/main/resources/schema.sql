-- Schema SQL para o banco H2

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

-- Tabela de destino
CREATE TABLE IF NOT EXISTS target_table (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    valor DECIMAL(10, 2) NOT NULL,
    processedo_em TIMESTAMP NOT NULL
);
