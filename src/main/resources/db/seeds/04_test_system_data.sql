-- =============================================================================
-- SEED 04: PAYLOADS E RELATÓRIOS (Cenários de Teste Consideráveis)
-- Objetivo: Validar processamento de JSONs e histórico de conselhos.
-- =============================================================================

-- 4.1. Payloads do Open Finance (Simulação de Retorno de API)
INSERT INTO open_finance_payloads (user_id, raw_payload, processed) VALUES 
((SELECT id FROM users WHERE email = 'perfil.clt.senior@email.com'), '{"bank": "Itaú", "transactions": [{"desc": "Supermercado", "val": 450.00}, {"desc": "Gasolina", "val": 300.00}]}', true),
((SELECT id FROM users WHERE email = 'loja.roupas.ltda@email.com'), '{"bank": "Santander", "transactions": [{"desc": "Venda Online", "val": 1500.00}, {"desc": "Logística", "val": 200.00}]}', true),
((SELECT id FROM users WHERE email = 'perfil.universitario@email.com'), '{"bank": "Caixa", "transactions": [{"desc": "Bolsa MEC", "val": 700.00}]}', true),
((SELECT id FROM users WHERE email = 'contato.mei@email.com'), '{"bank": "Inter", "transactions": [{"desc": "Entrega App", "val": 150.00}]}', false);

-- 4.2. Histórico de Relatórios de IA (Variedade de Conselhos)
INSERT INTO report_history (user_id, report_content, interval_type) VALUES 
((SELECT id FROM users WHERE email = 'perfil.clt.senior@email.com'), 'Seus gastos com lazer excederam 20% do orçamento este mês. Recomendamos realocação para reserva.', 'MONTHLY'),
((SELECT id FROM users WHERE email = 'perfil.clt.junior@email.com'), 'Você possui R$ 200,00 sobrando após gastos fixos. Deseja investir este valor?', 'WEEKLY'),
((SELECT id FROM users WHERE email = 'contato.mei@email.com'), 'Lembre-se de pagar o seu DAS até o dia 20 para evitar juros e multas.', 'MONTHLY'),
((SELECT id FROM users WHERE email = 'loja.roupas.ltda@email.com'), 'Seu fluxo de caixa indica sazonalidade negativa para o próximo mês. Reduza estoques.', 'BIWEEKLY'),
((SELECT id FROM users WHERE email = 'perfil.universitario@email.com'), 'Identificamos economia em transporte. Ótimo trabalho mantendo o orçamento estudantil!', 'WEEKLY');

COMMENT ON TABLE open_finance_payloads IS 'Massa de payloads brutos para teste de ETL e processamento de dados.';
COMMENT ON TABLE report_history IS 'Exemplos de outputs da IA para teste de visualização em mobile e web.';