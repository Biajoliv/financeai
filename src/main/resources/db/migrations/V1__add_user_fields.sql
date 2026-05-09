-- Migration V1: Adiciona campos name e system_role à tabela users
-- Execute manualmente no PostgreSQL antes de iniciar a aplicação em dev/prod

ALTER TABLE users ADD COLUMN IF NOT EXISTS name VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS system_role VARCHAR(20) DEFAULT 'FREE';

-- Garante que registros existentes tenham o valor padrão
UPDATE users SET system_role = 'FREE' WHERE system_role IS NULL;
