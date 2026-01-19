-- Dados de exemplo para as tabelas de origem
-- Usa ON CONFLICT DO NOTHING para evitar erros de chave duplicada

-- Inserindo dados na source_table_a
INSERT INTO source_table_a (id, nome, valor) VALUES (1, 'Produto A1', 100.50) ON CONFLICT (id) DO NOTHING;
INSERT INTO source_table_a (id, nome, valor) VALUES (2, 'Produto A2', 250.75) ON CONFLICT (id) DO NOTHING;
INSERT INTO source_table_a (id, nome, valor) VALUES (3, 'Produto A3', 89.90) ON CONFLICT (id) DO NOTHING;
INSERT INTO source_table_a (id, nome, valor) VALUES (4, 'Produto A4', 175.25) ON CONFLICT (id) DO NOTHING;

-- Inserindo dados na source_table_b
INSERT INTO source_table_b (id, nome, valor) VALUES (1, 'Serviço B1', 300.00) ON CONFLICT (id) DO NOTHING;
INSERT INTO source_table_b (id, nome, valor) VALUES (2, 'Serviço B2', 450.50) ON CONFLICT (id) DO NOTHING;
INSERT INTO source_table_b (id, nome, valor) VALUES (3, 'Serviço B3', 199.99) ON CONFLICT (id) DO NOTHING;
INSERT INTO source_table_b (id, nome, valor) VALUES (4, 'Serviço B4', 525.75) ON CONFLICT (id) DO NOTHING;
INSERT INTO source_table_b (id, nome, valor) VALUES (5, 'Serviço B5', 125.00) ON CONFLICT (id) DO NOTHING;
