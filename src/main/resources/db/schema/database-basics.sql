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
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    user_type system_role DEFAULT 'FREE',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    full_name_encrypted TEXT,
    holder_type account_holder_type NOT NULL,
    business_type company_type DEFAULT 'NONE',
    birth_date DATE,
    gender user_gender,
    education education_level,
    postal_code VARCHAR(10),
    has_open_finance_active BOOLEAN DEFAULT FALSE,
    report_preference report_interval DEFAULT 'MONTHLY',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE TABLE financial_institutions (
    id SERIAL PRIMARY KEY,
    ispb_code VARCHAR(8) UNIQUE NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    short_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE TABLE financial_accounts (
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    institution_id INT REFERENCES financial_institutions(id),
    account_type VARCHAR(20),
    balance DECIMAL(15,2) DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE TABLE system_configs (
    id SERIAL PRIMARY KEY,
    config_key VARCHAR(50) UNIQUE NOT NULL,
    config_value VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

-- 4. MOVIMENTAÇÃO E IA
CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    account_id INT REFERENCES financial_accounts(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    category VARCHAR(50), 
    transaction_date DATE NOT NULL,
    is_open_finance BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE TABLE open_finance_payloads (
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    raw_payload JSONB NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE TABLE report_history (
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    report_content TEXT NOT NULL,
    interval_type report_interval,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

-- 5. PERFORMANCE E UTILITÁRIOS
CREATE INDEX idx_trans_user_date ON transactions(user_id, transaction_date);
CREATE INDEX idx_trans_account ON transactions(account_id);

CREATE OR REPLACE VIEW view_user_spending_summary AS
SELECT 
    user_id,
    category,
    SUM(amount) AS total_spent,
    COUNT(id) AS transaction_count,
    MAX(transaction_date) AS last_transaction,
    CURRENT_TIMESTAMP AS calculated_at
FROM transactions
WHERE deleted_at IS NULL 
GROUP BY user_id, category;

COMMENT ON VIEW view_user_spending_summary IS 'Resumo de gastos por categoria para alimentar o motor de recomendações, ignorando registros deletados.';

-- =============================================================================
-- 6. AUTOMAÇÃO E INTEGRIDADE (MELHORIAS DE QA & AUDITORIA)
-- =============================================================================

-- Função Universal de Auditoria (Gerencia o updated_at automaticamente)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    -- Garante que o updated_at sempre reflita o momento exato da alteração
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Aplicação de Triggers em todas as tabelas (Rastreabilidade Total)
-- Identidade e Perfil
CREATE TRIGGER trg_update_users_modtime BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_update_profiles_modtime BEFORE UPDATE ON user_profiles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Infraestrutura Financeira
CREATE TRIGGER trg_update_inst_modtime BEFORE UPDATE ON financial_institutions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_update_accounts_modtime BEFORE UPDATE ON financial_accounts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_update_configs_modtime BEFORE UPDATE ON system_configs FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Movimentações e IA (Crítico para o Motor de Cálculo)
CREATE TRIGGER trg_update_transactions_modtime BEFORE UPDATE ON transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_update_payloads_modtime BEFORE UPDATE ON open_finance_payloads FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trg_update_reports_modtime BEFORE UPDATE ON report_history FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- -----------------------------------------------------------------------------
-- Constraints de Integridade (Blindagem do Motor de Cálculo)
-- -----------------------------------------------------------------------------

-- Impede que o motor processe transações com valor zero (lixo de dados)
ALTER TABLE transactions ADD CONSTRAINT chk_amount_not_zero CHECK (amount <> 0);

-- Regra de Negócio: Impede saldos negativos absurdos sem linha de crédito
ALTER TABLE financial_accounts ADD CONSTRAINT chk_balance_limit CHECK (balance >= -1000.00);

-- -----------------------------------------------------------------------------
-- Procedure de Sandbox (Exclusivo para Testes de QA)
-- -----------------------------------------------------------------------------
CREATE OR REPLACE PROCEDURE reset_test_data()
LANGUAGE plpgsql
AS $$
BEGIN
    -- Limpa apenas dados transacionais, mantendo usuários e configurações
    DELETE FROM transactions;
    DELETE FROM report_history;
    DELETE FROM open_finance_payloads;
    -- Reinicia sequenciais para manter logs limpos
    ALTER SEQUENCE IF EXISTS transactions_id_seq RESTART WITH 1;
    RAISE NOTICE 'Ambiente de testes limpo com sucesso.';
END;
$$;

COMMENT ON PROCEDURE reset_test_data IS 'Limpa movimentações e relatórios para reiniciar testes sem apagar usuários e contas.';