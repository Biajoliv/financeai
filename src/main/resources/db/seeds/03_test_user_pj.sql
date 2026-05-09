-- =============================================================================
-- SEED 03: USUÁRIOS PESSOA JURÍDICA (UUID & AUDIT)
-- =============================================================================

-- 1. Usuários PJ
INSERT INTO users (email, password_hash, user_type) VALUES 
('contato.mei@email.com', 'hash6', 'FREE'),
('loja.roupas.ltda@email.com', 'hash7', 'PREMIUM'),
('consultoria.ti@email.com', 'hash8', 'PREMIUM'),
('cafeteria.artesanal@email.com', 'hash9', 'FREE'),
('agencia.marketing@email.com', 'hash10', 'PREMIUM');

-- 2. Perfis PJ (Vinculados via Subquery de Email)
INSERT INTO user_profiles (user_id, full_name_encrypted, holder_type, business_type, postal_code, has_open_finance_active, created_at) VALUES 
((SELECT id FROM users WHERE email = 'contato.mei@email.com'), 'João MEI Entregas', 'PJ', 'MEI', '36700-000', true, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE email = 'loja.roupas.ltda@email.com'), 'Moda Express LTDA', 'PJ', 'LTDA', '01153-000', true, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE email = 'consultoria.ti@email.com'), 'Tech Solutions SA', 'PJ', 'SA', '80010-000', true, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE email = 'cafeteria.artesanal@email.com'), 'Grão Café ME', 'PJ', 'MEI', '36700-000', false, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE email = 'agencia.marketing@email.com'), 'Agência Alpha', 'PJ', 'LTDA', '22041-001', true, CURRENT_TIMESTAMP);

-- 3. Contas Bancárias PJ
INSERT INTO financial_accounts (user_id, institution_id, account_type, balance, created_at) VALUES 
((SELECT id FROM users WHERE email = 'contato.mei@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Inter'), 'PJ_CORRENTE', 2500.00, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE email = 'loja.roupas.ltda@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Santander'), 'PJ_CORRENTE', 45000.00, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE email = 'consultoria.ti@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Itaú'), 'PJ_CORRENTE', 120000.00, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE email = 'cafeteria.artesanal@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Banco do Brasil'), 'PJ_CORRENTE', 8900.00, CURRENT_TIMESTAMP),
((SELECT id FROM users WHERE email = 'agencia.marketing@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Nubank'), 'PJ_CORRENTE', 25000.00, CURRENT_TIMESTAMP);

-- 4. Transações PJ (Críticas para validação de impostos e fluxo de caixa)
INSERT INTO transactions (user_id, account_id, description, amount, category, transaction_date, is_open_finance, created_at) VALUES 
-- MEI: Pagamento de Imposto DAS
((SELECT id FROM users WHERE email = 'contato.mei@email.com'), 
 (SELECT id FROM financial_accounts WHERE user_id = (SELECT id FROM users WHERE email = 'contato.mei@email.com') LIMIT 1), 
 'PAGTO DAS', -72.00, 'ESSENTIAL', '2026-05-20', true, CURRENT_TIMESTAMP),

-- LTDA: Fornecedor
((SELECT id FROM users WHERE email = 'loja.roupas.ltda@email.com'), 
 (SELECT id FROM financial_accounts WHERE user_id = (SELECT id FROM users WHERE email = 'loja.roupas.ltda@email.com') LIMIT 1), 
 'FORNECEDOR TEXTIL', -12000.00, 'ESSENTIAL', '2026-05-05', true, CURRENT_TIMESTAMP),

-- SA: Recebimento de Contrato
((SELECT id FROM users WHERE email = 'consultoria.ti@email.com'), 
 (SELECT id FROM financial_accounts WHERE user_id = (SELECT id FROM users WHERE email = 'consultoria.ti@email.com') LIMIT 1), 
 'RECEBIMENTO PROJETO CLOUD', 55000.00, 'REVENUE', '2026-05-10', true, CURRENT_TIMESTAMP);

COMMENT ON TABLE transactions IS 'Massa de dados PJ para teste de análise de fluxo de caixa e impostos.';