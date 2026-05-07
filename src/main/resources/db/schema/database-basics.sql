-- =============================================================================
-- DATABASE BASICS - MOTOR DE CÁLCULO E IDENTIDADE
-- Branch: feature/database-basics
-- =============================================================================

-- 1. LIMPEZA TOTAL (Para garantir execução limpa)
DROP VIEW IF EXISTS view_user_spending_summary;
DROP TABLE IF EXISTS report_history, transactions, open_finance_payloads, financial_accounts, financial_institutions, user_profiles, users CASCADE;
DROP TYPE IF EXISTS system_role, account_holder_type, company_type, user_gender, education_level, report_interval CASCADE;

-- 2. EXTENSÕES E ENUMS
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TYPE system_role AS ENUM ('FREE', 'PREMIUM', 'ADMIN', 'SUPPORT');
CREATE TYPE account_holder_type AS ENUM ('PF', 'PJ');
CREATE TYPE company_type AS ENUM ('NONE', 'MEI', 'EI', 'LTDA', 'SA', 'UNSPECIFIED');
CREATE TYPE user_gender AS ENUM ('M', 'F', 'NB', 'OTHER', 'PREFER_NOT_TO_SAY');
CREATE TYPE education_level AS ENUM ('BASIC', 'HIGH_SCHOOL', 'UNDERGRADUATE', 'GRADUATE', 'DOCTORATE');
CREATE TYPE report_interval AS ENUM ('WEEKLY', 'BIWEEKLY', 'MONTHLY');

-- 3. TABELAS DE IDENTIDADE E INFRA
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    user_type system_role DEFAULT 'FREE',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_profiles (
    user_id INT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    full_name_encrypted TEXT,
    holder_type account_holder_type NOT NULL,
    business_type company_type DEFAULT 'NONE',
    birth_date DATE,
    gender user_gender,
    education education_level,
    postal_code VARCHAR(10),
    has_open_finance_active BOOLEAN DEFAULT FALSE,
    report_preference report_interval DEFAULT 'MONTHLY'
);

CREATE TABLE financial_institutions (
    id SERIAL PRIMARY KEY,
    ispb_code VARCHAR(8) UNIQUE NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    short_name VARCHAR(50)
);

CREATE TABLE financial_accounts (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    institution_id INT REFERENCES financial_institutions(id),
    account_type VARCHAR(20),
    balance DECIMAL(15,2) DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. MOVIMENTAÇÃO E IA (CORRIGIDA COM ACCOUNT_ID)
CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    account_id INT REFERENCES financial_accounts(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    category VARCHAR(50), 
    transaction_date DATE NOT NULL,
    is_open_finance BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE open_finance_payloads (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    raw_payload JSONB NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE report_history (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    report_content TEXT NOT NULL,
    interval_type report_interval,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. PERFORMANCE E UTILITÁRIOS
CREATE INDEX idx_trans_user_date ON transactions(user_id, transaction_date);
CREATE INDEX idx_trans_account ON transactions(account_id);

CREATE VIEW view_user_spending_summary AS
SELECT 
    u.email, p.holder_type, t.category,
    SUM(t.amount) as total_amount, COUNT(t.id) as tx_count
FROM users u
JOIN user_profiles p ON u.id = p.user_id
JOIN transactions t ON u.id = t.user_id
GROUP BY u.email, p.holder_type, t.category;

-- =============================================================================
-- 6. AUTOMAÇÃO E INTEGRIDADE (MELHORIAS DE QA)
-- =============================================================================

-- Função para atualizar o timestamp de 'updated_at' automaticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Gatilhos (Triggers) para garantir que o banco gerencie as datas sozinho
CREATE TRIGGER trg_update_users_modtime BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_update_profiles_modtime BEFORE UPDATE ON user_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_update_accounts_modtime BEFORE UPDATE ON financial_accounts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_update_transactions_modtime BEFORE UPDATE ON transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Constraints de Validação (Blindagem contra "lixo" no Motor de Cálculo)
ALTER TABLE transactions ADD CONSTRAINT chk_amount_not_zero CHECK (amount <> 0);
ALTER TABLE financial_accounts ADD CONSTRAINT chk_balance_positive CHECK (balance >= -500.00); -- Ex: limite de cheque especial

-- Procedure de Limpeza para Testes Rápidos (Sandbox)
-- Uso: CALL reset_test_data();
CREATE OR REPLACE PROCEDURE reset_test_data()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM transactions;
    DELETE FROM report_history;
    DELETE FROM open_finance_payloads;
    ALTER SEQUENCE transactions_id_seq RESTART WITH 1;
END;
$$;

COMMENT ON PROCEDURE reset_test_data IS 'Limpa movimentações e relatórios para reiniciar testes sem apagar usuários e contas.';