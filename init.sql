-- ==========================================
-- EXTENSÕES
-- ==========================================
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ==========================================
-- TABELA RAG — documentos para busca vetorial
-- tenant_id garante isolamento por usuário
-- embedding vector(1536) = dimensão OpenAI
-- ==========================================
CREATE TABLE IF NOT EXISTS rag_documents (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(64)  NOT NULL DEFAULT 'global',
    chunk_id    VARCHAR(100) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    source_type VARCHAR(50),
    embedding   vector(1536),
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- Índice para busca vetorial eficiente
CREATE INDEX IF NOT EXISTS idx_rag_embedding
    ON rag_documents USING ivfflat (embedding vector_cosine_ops);

-- Índice para filtro por tenant
CREATE INDEX IF NOT EXISTS idx_rag_tenant
    ON rag_documents (tenant_id);

-- ==========================================
-- DOCUMENTOS INICIAIS DO RAG
-- Base de conhecimento financeiro
-- ==========================================
INSERT INTO rag_documents (tenant_id, chunk_id, title, content, source_type) VALUES
('global', 'educ_fin_001', 'Metas financeiras',
 'Para alcançar uma meta financeira, calcule o valor total, o prazo e a economia mensal necessária. Divida o valor pelo número de meses para saber quanto guardar por mês.',
 'educacao_financeira'),

('global', 'educ_fin_002', 'Regra 50/30/20',
 'A regra 50/30/20 organiza a renda em: 50% necessidades, 30% desejos e 20% poupança. Ajuda a visualizar limites de gasto por categoria.',
 'educacao_financeira'),

('global', 'educ_fin_003', 'Gastos invisíveis',
 'Pequenos gastos recorrentes como assinaturas e delivery podem comprometer o orçamento sem serem percebidos. Some todas as assinaturas mensais para ter clareza.',
 'educacao_financeira'),

('global', 'educ_fin_004', 'Reserva de emergência',
 'Antes de qualquer meta, construa uma reserva de emergência equivalente a 3 a 6 meses de despesas. Isso evita dívidas em imprevistos.',
 'educacao_financeira'),

('global', 'educ_fin_005', 'Automação de poupança',
 'Configure transferência automática no dia do pagamento. Pague a si mesmo primeiro — separe a poupança antes de gastar.',
 'educacao_financeira'),

('global', 'caixa_prod_001', 'Poupança Caixa',
 'A poupança é indicada para objetivos de curto prazo com necessidade de liquidez imediata. Rendimento mensal na data de aniversário.',
 'produto_financeiro'),

('global', 'caixa_prod_002', 'CDB',
 'O CDB rende mais que a poupança para valores acima de R$1.000. Verifique o prazo mínimo antes de investir.',
 'produto_financeiro'),

('global', 'comp_fin_001', 'Perfil de crise',
 'Usuários em perfil de crise precisam de alertas simples, ações imediatas e priorização de despesas essenciais como moradia e alimentação.',
 'comportamento_financeiro'),

('global', 'comp_fin_002', 'Perfil objetivo',
 'Usuários com objetivo definido precisam de projeção de prazo realista e plano de ação mensal com metas claras.',
 'comportamento_financeiro'),

('global', 'comp_fin_003', 'Controle de delivery',
 'Gastos com delivery e alimentação fora de casa costumam ser os maiores vilões do orçamento. Definir um limite semanal ajuda no controle.',
 'comportamento_financeiro');
