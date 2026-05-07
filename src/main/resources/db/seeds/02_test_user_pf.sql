-- SEED: 5 Usuários Pessoa Física
INSERT INTO users (email, password_hash, user_type) VALUES 
('perfil.universitario@email.com', 'hash1', 'FREE'),
('perfil.clt.junior@email.com', 'hash2', 'FREE'),
('perfil.clt.senior@email.com', 'hash3', 'PREMIUM'),
('perfil.aposentado@email.com', 'hash4', 'FREE'),
('perfil.investidor@email.com', 'hash5', 'PREMIUM');

-- Perfis e Contas
INSERT INTO user_profiles (user_id, full_name_encrypted, holder_type, education, postal_code, has_open_finance_active) VALUES 
((SELECT id FROM users WHERE email = 'perfil.universitario@email.com'), 'Carlos Edu', 'PF', 'UNDERGRADUATE', '36700-000', true),
((SELECT id FROM users WHERE email = 'perfil.clt.junior@email.com'), 'Mariana Souza', 'PF', 'GRADUATE', '30110-000', true),
((SELECT id FROM users WHERE email = 'perfil.clt.senior@email.com'), 'Ricardo Alves', 'PF', 'GRADUATE', '01310-000', true),
((SELECT id FROM users WHERE email = 'perfil.aposentado@email.com'), 'Beatriz Helena', 'PF', 'BASIC', '36700-000', false), -- SEM OPEN FINANCE
((SELECT id FROM users WHERE email = 'perfil.investidor@email.com'), 'Marcos Vinicius', 'PF', 'DOCTORATE', '22041-001', true);

-- Contas em Bancos Diferentes
INSERT INTO financial_accounts (user_id, institution_id, account_type, balance) VALUES 
((SELECT id FROM users WHERE email = 'perfil.universitario@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Caixa'), 'CORRENTE', 450.00),
((SELECT id FROM users WHERE email = 'perfil.clt.junior@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Nubank'), 'CORRENTE', 2800.00),
((SELECT id FROM users WHERE email = 'perfil.clt.senior@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Itaú'), 'CORRENTE', 15200.00),
((SELECT id FROM users WHERE email = 'perfil.aposentado@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Bradesco'), 'CORRENTE', 3100.00),
((SELECT id FROM users WHERE email = 'perfil.investidor@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'BTG Pactual'), 'INVESTIMENTO', 85000.00);

-- Transações estratégicas para o motor (Ajustadas com Subqueries para evitar erros de ID)
INSERT INTO transactions (user_id, account_id, description, amount, category, transaction_date, is_open_finance) VALUES 
-- Universitário: Gasto Essencial alto vs Renda
((SELECT id FROM users WHERE email = 'perfil.universitario@email.com'), (SELECT id FROM financial_accounts WHERE user_id = (SELECT id FROM users WHERE email = 'perfil.universitario@email.com')), 'PIX REPUBLICA', -500.00, 'ESSENTIAL', '2026-05-01', true),
-- CLT Senior: Muitos gastos opcionais (Lazer/Restaurante)
((SELECT id FROM users WHERE email = 'perfil.clt.senior@email.com'), (SELECT id FROM financial_accounts WHERE user_id = (SELECT id FROM users WHERE email = 'perfil.clt.senior@email.com')), 'RESTAURANTE JAPA', -350.00, 'OPTIONAL', '2026-05-02', true),
-- Aposentado: Inserção MANUAL (is_open_finance = false)
((SELECT id FROM users WHERE email = 'perfil.aposentado@email.com'), (SELECT id FROM financial_accounts WHERE user_id = (SELECT id FROM users WHERE email = 'perfil.aposentado@email.com')), 'COMPRA FARMACIA', -120.00, 'ESSENTIAL', '2026-05-03', false);