-- =============================================================================
-- SEED 02: USUÁRIOS PESSOA FÍSICA (UUID & AUDIT)
-- =============================================================================

INSERT INTO users (email, password_hash, user_type, created_at, updated_at) VALUES 
('perfil.universitario@email.com', 'hash1', 'FREE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('perfil.clt.junior@email.com', 'hash2', 'FREE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('perfil.clt.senior@email.com', '$2a$10$8.VocS6Z7VfJ.jZ.Lp6EBeG6vU4h/H8C.p.Y8Z5V6G8V6G8V6G8V6', 'PREMIUM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('perfil.aposentado@email.com', 'hash4', 'FREE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('perfil.investidor@email.com', 'hash5', 'PREMIUM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Perfis vinculados via Email (mais seguro que ID fixo)
INSERT INTO user_profiles (user_id, full_name_encrypted, holder_type, education, postal_code, has_open_finance_active, created_at, updated_at) VALUES 
((SELECT id FROM users WHERE email = 'perfil.universitario@email.com'), 'Carlos Edu', 'PF', 'UNDERGRADUATE', '36700-000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE email = 'perfil.clt.junior@email.com'), 'Mariana Souza', 'PF', 'GRADUATE', '30110-000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE email = 'perfil.clt.senior@email.com'), 'Ricardo Alves', 'PF', 'GRADUATE', '01310-000', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE email = 'perfil.aposentado@email.com'), 'Beatriz Helena', 'PF', 'BASIC', '36700-000', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE email = 'perfil.investidor@email.com'), 'Marcos Vinicius', 'PF', 'DOCTORATE', '22041-001', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

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