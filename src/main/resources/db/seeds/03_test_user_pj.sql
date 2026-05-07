-- SEED: 5 Usuários Pessoa Jurídica
INSERT INTO users (email, password_hash, user_type) VALUES 
('contato.mei@email.com', 'hash6', 'FREE'),
('loja.roupas.ltda@email.com', 'hash7', 'PREMIUM'),
('consultoria.ti@email.com', 'hash8', 'PREMIUM'),
('cafeteria.artesanal@email.com', 'hash9', 'FREE'),
('agencia.marketing@email.com', 'hash10', 'PREMIUM');

-- Perfis e Contas PJ
INSERT INTO user_profiles (user_id, full_name_encrypted, holder_type, business_type, postal_code, has_open_finance_active) VALUES 
((SELECT id FROM users WHERE email = 'contato.mei@email.com'), 'João MEI Entregas', 'PJ', 'MEI', '36700-000', true),
((SELECT id FROM users WHERE email = 'loja.roupas.ltda@email.com'), 'Moda Express LTDA', 'PJ', 'LTDA', '01153-000', true),
((SELECT id FROM users WHERE email = 'consultoria.ti@email.com'), 'Tech Solutions SA', 'PJ', 'SA', '80010-000', true),
((SELECT id FROM users WHERE email = 'cafeteria.artesanal@email.com'), 'Grão Café ME', 'PJ', 'MEI', '36700-000', false), -- SEM OPEN FINANCE
((SELECT id FROM users WHERE email = 'agencia.marketing@email.com'), 'Viralize Mkt', 'PJ', 'LTDA', '22250-040', true);

-- Contas em Bancos Variados
INSERT INTO financial_accounts (user_id, institution_id, account_type, balance) VALUES 
((SELECT id FROM users WHERE email = 'contato.mei@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Inter'), 'PJ_CORRENTE', 1200.00),
((SELECT id FROM users WHERE email = 'loja.roupas.ltda@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Santander'), 'PJ_CORRENTE', 45000.00),
((SELECT id FROM users WHERE email = 'consultoria.ti@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Itaú'), 'PJ_CORRENTE', 120000.00),
((SELECT id FROM users WHERE email = 'cafeteria.artesanal@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Banco do Brasil'), 'PJ_CORRENTE', 8900.00),
((SELECT id FROM users WHERE email = 'agencia.marketing@email.com'), (SELECT id FROM financial_institutions WHERE short_name = 'Nubank'), 'PJ_CORRENTE', 25000.00);

-- Transações PJ (Ajustadas com Subqueries para evitar o erro do print)
INSERT INTO transactions (user_id, account_id, description, amount, category, transaction_date, is_open_finance) VALUES 
-- MEI: Imposto DAS
((SELECT id FROM users WHERE email = 'contato.mei@email.com'), (SELECT id FROM financial_accounts WHERE user_id = (SELECT id FROM users WHERE email = 'contato.mei@email.com')), 'PAGTO DAS', -72.00, 'ESSENTIAL', '2026-05-20', true),
-- LTDA: Fornecedor de Tecidos
((SELECT id FROM users WHERE email = 'loja.roupas.ltda@email.com'), (SELECT id FROM financial_accounts WHERE user_id = (SELECT id FROM users WHERE email = 'loja.roupas.ltda@email.com')), 'FORNECEDOR TEXTIL', -12000.00, 'ESSENTIAL', '2026-05-15', true),
-- Cafeteria: Manual (Compra de Café)
((SELECT id FROM users WHERE email = 'cafeteria.artesanal@email.com'), (SELECT id FROM financial_accounts WHERE user_id = (SELECT id FROM users WHERE email = 'cafeteria.artesanal@email.com')), 'COMPRA SACAS CAFE', -2500.00, 'ESSENTIAL', '2026-05-10', false);