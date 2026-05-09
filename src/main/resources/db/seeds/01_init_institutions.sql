-- =============================================================================
-- SEED 01: INSTITUIÇÕES FINANCEIRAS (Base para Mapeamento Open Finance)
-- =============================================================================

INSERT INTO financial_institutions (ispb_code, bank_name, short_name, created_at, updated_at) VALUES 
('00360305', 'CAIXA ECONOMICA FEDERAL', 'Caixa', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('60701190', 'ITAÚ UNIBANCO S.A.', 'Itaú', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('00000000', 'BANCO DO BRASIL S.A.', 'Banco do Brasil', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('90400888', 'BANCO SANTANDER (BRASIL) S.A.', 'Santander', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('60746948', 'BANCO BRADESCO S.A.', 'Bradesco', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('18727053', 'BANCO INTER S.A.', 'Inter', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('30739571', 'NU PAGAMENTOS S.A.', 'Nubank', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('11725176', 'BANCO BTG PACTUAL S.A.', 'BTG Pactual', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('07204481', 'BANCO BMG S.A.', 'BMG', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

COMMENT ON TABLE financial_institutions IS 'Lista de instituições para teste de mapeamento de payloads Open Finance';