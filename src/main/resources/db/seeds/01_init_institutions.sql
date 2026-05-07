-- SEED: Instituições Financeiras (Catálogo Nacional)
INSERT INTO financial_institutions (ispb_code, bank_name, short_name) VALUES 
('00360305', 'CAIXA ECONOMICA FEDERAL', 'Caixa'),
('60701190', 'ITAÚ UNIBANCO S.A.', 'Itaú'),
('00000000', 'BANCO DO BRASIL S.A.', 'Banco do Brasil'),
('90400888', 'BANCO SANTANDER (BRASIL) S.A.', 'Santander'),
('60746948', 'BANCO BRADESCO S.A.', 'Bradesco'),
('18727053', 'BANCO INTER S.A.', 'Inter'),
('30739571', 'NU PAGAMENTOS S.A.', 'Nubank'),
('11725176', 'BANCO BTG PACTUAL S.A.', 'BTG Pactual'),
('07204481', 'BANCO BMG S.A.', 'BMG');

COMMENT ON TABLE financial_institutions IS 'Lista de instituições para teste de mapeamento de payloads Open Finance';