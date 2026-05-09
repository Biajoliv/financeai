-- Migration V2: Adiciona campos deletedAt, isOpenFinance e ajusta Transaction JPA

-- A entidade JPA Transaction usa transaction_date como nome de coluna
-- Se a tabela já existe com 'date', renomear:
ALTER TABLE transactions RENAME COLUMN IF EXISTS date TO transaction_date;

-- Adiciona campos novos se não existirem
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS is_open_finance BOOLEAN DEFAULT FALSE;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP DEFAULT NULL;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS type VARCHAR(20);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS is_essential BOOLEAN DEFAULT FALSE;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS priority INTEGER DEFAULT 1;

-- Ajusta constraint de amount para aceitar negativos (JPA usa @Positive, mas depende da lógica)
-- Se a constraint chk_amount_not_zero já existe, manter; transaction_date deve ser NOT NULL
ALTER TABLE transactions ALTER COLUMN transaction_date SET NOT NULL;
