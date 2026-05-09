-- Migration V4: Corrige tipo do id de transactions de INTEGER para VARCHAR(36) (UUID)
--
-- PROBLEMA:
--   transactions.id era SERIAL (INTEGER), mas a entidade JPA Transaction.java usa
--   @GeneratedValue(UUID), gerando strings como "550e8400-e29b-41d4-a716-446655440000".
--   O INSERT falhava com "invalid input syntax for type integer" no PostgreSQL.
--   O teste H2 passava porque ddl-auto=create-drop cria a tabela pela entidade JPA.
--
-- IMPACTO NOS DADOS EXISTENTES:
--   As 6 linhas de seed possuem type=NULL e category com valores incompatíveis com o
--   enum JPA (ESSENTIAL/OPTIONAL/REVENUE vs FOOD/HOUSING/SALARY...). São inutilizáveis
--   pelo motor — removidas antes da alteração.
--
-- Execute manualmente no PostgreSQL (Render) antes de reiniciar a aplicação.

-- 0. Remove view que depende de transactions.id (necessário antes do ALTER COLUMN TYPE)
DROP VIEW IF EXISTS view_user_spending_summary;

-- 1. Remove dados de seed incompatíveis com o enum JPA
TRUNCATE TABLE transactions;

-- 2. Desvincula a column do SERIAL (transactions_id_seq)
ALTER TABLE transactions ALTER COLUMN id DROP DEFAULT;

-- 3. Converte INTEGER → VARCHAR(36) para receber UUIDs gerados pelo Hibernate
ALTER TABLE transactions ALTER COLUMN id TYPE VARCHAR(36) USING id::text;

-- 4. Remove a sequence SERIAL que ficou órfã
DROP SEQUENCE IF EXISTS transactions_id_seq;

-- 5. Garante NOT NULL em type (entidade JPA tem @Column(nullable=false))
--    Safe porque a tabela está vazia após o TRUNCATE
ALTER TABLE transactions ALTER COLUMN type SET NOT NULL;

-- 6. Recria a view com o schema corrigido
CREATE OR REPLACE VIEW view_user_spending_summary AS
SELECT
    user_id,
    category,
    SUM(amount)           AS total_spent,
    COUNT(id)             AS transaction_count,
    MAX(transaction_date) AS last_transaction,
    CURRENT_TIMESTAMP     AS calculated_at
FROM transactions
WHERE deleted_at IS NULL
GROUP BY user_id, category;
