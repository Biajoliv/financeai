-- Migration V3: Cria tabelas para persistência de simulações e relatórios

CREATE TABLE IF NOT EXISTS simulations (
    id VARCHAR(36) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_data TEXT,
    scenario_results TEXT,
    diagnostic_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_simulation_user ON simulations(user_id);

CREATE TABLE IF NOT EXISTS report_records (
    id VARCHAR(36) PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    interval_type VARCHAR(20) NOT NULL,
    header_data TEXT,
    report_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP DEFAULT NULL
);

CREATE INDEX IF NOT EXISTS idx_report_user ON report_records(user_id);
