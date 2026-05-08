-- =============================================================================
-- SEED 05: CONFIGURAÇÕES DO MOTOR (Substitui Map.of no Java)
-- =============================================================================

INSERT INTO system_configs (config_key, config_value, description, created_at, updated_at) VALUES 
('ALGO_OPTIMIZER', 'Knapsack 0/1', 'Algoritmo de otimização de gastos', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ALGO_COMPLEXITY', 'O(nW)', 'Complexidade Big O do algoritmo atual', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WEIGHT_ESSENTIAL', '10', 'Peso de prioridade para gastos essenciais', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WEIGHT_IMPORTANT', '5', 'Peso de prioridade para gastos importantes', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WEIGHT_OPTIONAL', '1', 'Peso de prioridade para gastos opcionais', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TAX_IOF', '0.0038', 'Alíquota padrão de IOF', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TAX_IR_SHORT_TERM', '0.225', 'Alíquota de IR para curto prazo (até 180 dias)', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

COMMENT ON TABLE system_configs IS 'Massa de dados para configuração dinâmica do motor de cálculo.';