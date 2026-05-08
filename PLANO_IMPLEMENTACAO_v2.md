# FinanceAI — Plano de Implementação v2 (Revisado)

**Data:** 2026-05-08 | **Status:** ✅ CONCLUÍDO

---

## 🔍 Diagnóstico do Estado Atual

### O que já existe e funciona
| Componente | Status | Observações |
|---|---|---|
| `POST /auth/register` | ✅ Funcional | Cria User; `name` capturado mas ignorado |
| `POST /auth/login` | ✅ Funcional | JWT gerado corretamente |
| `DELETE /auth/account` | ✅ Funcional | Soft delete ok |
| `POST /api/analysis/simulate` | ✅ Funcional | Usa transações do body (aceita input do cliente/IA) |
| `POST /api/analysis/report` | ⚠️ Parcial | Usa MockIngestionService, sem comparação 6 períodos |
| `GET /api/analysis/status` | ✅ Funcional | Retorna OPERATIONAL |
| `DELETE /api/analysis/{id}` | ✅ Funcional | Soft delete de diagnóstico |
| GlobalExceptionHandler | ✅ Existe | Mas expõe email em mensagem de erro |

### Problemas Identificados e Prioridade
| # | Problema | Impacto | Prioridade |
|---|---|---|---|
| 1 | `name` do registro não é salvo em lugar nenhum | Alto | P1 |
| 2 | Sem UserProfile → sem open_finance, report_interval, holder_type | Alto | P1 |
| 3 | Sem rotas GET/PUT para usuário e perfil | Alto | P1 |
| 4 | Sem transações reais no banco → motor usa mock | Alto | P1 |
| 5 | Sem rota para gerar transações aleatórias | Alto | P1 |
| 6 | Report não compara 6 períodos | Médio | P2 |
| 7 | Simulações e relatórios não são persistidos | Médio | P2 |
| 8 | `GlobalExceptionHandler` expõe email em "Email já cadastrado" | Médio | P1 |

---

## ⚠️ Restrições Confirmadas

| Restrição | Detalhe |
|---|---|
| **NÃO tocar JwtService, JwtFilter, SecurityConfig** | Login é crítico |
| **`/auth/**` é `permitAll()`** | Todas as rotas `/auth/**` são públicas por design! |
| **ddl-auto=none em dev** | Hibernate NÃO cria tabelas — SQL migrations necessárias |
| **ddl-auto=create-drop em test** | H2 cria tabelas de entidades JPA automaticamente |
| **Soft delete** | `deleted_at IS NOT NULL` = excluído lógicamente |
| **Campos imutáveis** | email, password, cpf_cnpj, account_holder_type |

### ⚠️ Decisão Sobre Rotas de Usuário/Perfil
`/auth/**` é `permitAll()` no SecurityConfig. Para NÃO mexer no SecurityConfig e ainda ter rotas protegidas:
- Rotas de usuário → **`/api/user`** (protegido por `anyRequest().authenticated()`)
- Rotas de perfil → **`/api/profile`** (protegido automaticamente)
- DELETE conta → `/auth/account` já existe

---

## 🗺️ Rotas Finais (Definitivas)

### Públicas (sem JWT)
| Método | Rota | Status |
|---|---|---|
| `POST` | `/auth/register` | ✅ Existente |
| `POST` | `/auth/login` | ✅ Existente |
| `GET` | `/api/analysis/status` | ✅ Existente |

### Protegidas (requerem JWT Bearer)

**Usuário**
| Método | Rota | Status |
|---|---|---|
| `GET` | `/api/user` | 🆕 Novo |
| `PUT` | `/api/user` | 🆕 Novo |
| `DELETE` | `/auth/account` | ✅ Existente |

**Perfil**
| Método | Rota | Status |
|---|---|---|
| `GET` | `/api/profile` | 🆕 Novo |
| `PUT` | `/api/profile` | 🆕 Novo |

**Transações**
| Método | Rota | Status |
|---|---|---|
| `POST` | `/api/transaction/generate` | 🆕 Novo |
| `GET` | `/api/transaction` | 🆕 Novo |
| `DELETE` | `/api/transaction/{id}` | 🆕 Novo |

**Relatórios**
| Método | Rota | Status |
|---|---|---|
| `GET` | `/api/report/list` | 🆕 Novo |
| `GET` | `/api/report/{id}` | 🆕 Novo |
| `POST` | `/api/analysis/report` | ⬆️ Update (6 períodos + persist) |

**Análise**
| Método | Rota | Status |
|---|---|---|
| `POST` | `/api/analysis/simulate` | ✅ Existente |
| `GET` | `/api/analysis/simulation/list` | 🆕 Novo |
| `GET` | `/api/analysis/simulation/{id}` | 🆕 Novo |
| `GET` | `/api/analysis/config/weights` | ✅ Existente |
| `GET` | `/api/analysis/config/tax-rules` | ✅ Existente |
| `DELETE` | `/api/analysis/{id}` | ✅ Existente |

---

## 📐 Formatos de Resposta (Padrão Obrigatório)

```json
// Sucesso
{ "message": "...", "data": {...}, "timestamp": "2026-05-08T16:46:24" }

// Sem dados
{ "message": "Não foram encontrados dados.", "data": null, "timestamp": "..." }

// Erro
{ "message": "Mensagem segura sem dados internos.", "timestamp": "..." }
```

---

## 🔒 Validações de Campos Imutáveis

```
PUT /api/user ou /api/profile:
  email enviado       → 400 "E-mail não pode ser alterado."
  password enviado    → 400 "Senha não pode ser alterada."
  cpf_cnpj enviado    → 400 "Documento não pode ser alterado."
  holder_type enviado → 400 "Tipo de titular não pode ser alterado."
```

---

## 📊 Estrutura de Relatório com 6 Períodos

```json
{
  "message": "Relatório gerado com sucesso.",
  "data": {
    "header": {
      "title": "Relatório Mensal",
      "subtitle": "Período: Dez 2025 – Mai 2026",
      "name": "João Silva"
    },
    "report": {
      "reportBalance": 1594.10,
      "comparison": {
        "HOUSING": [2200.00, 2150.00, 2100.00, 2100.00, 2050.00, 2200.00],
        "FOOD": [450.00, 480.00, 510.00, 390.00, 420.00, 460.00]
      },
      "alerts": ["Risco de liquidez detectado no curto prazo."]
    }
  },
  "timestamp": "2026-05-08T18:00:00"
}
```

---

## 🧱 Arquitetura de Implementação

### Novas Entidades JPA
```
domain/entity/
  UserProfile.java        ← mapeia user_profiles (PostgreSQL)
  SimulationRecord.java   ← persiste simulações
  ReportRecord.java       ← persiste relatórios

domain/entity/Transaction.java  ← adicionar deletedAt + isOpenFinance
domain/entity/User.java         ← adicionar name + systemRole
```

### Novos Repositórios
```
infrastructure/persistence/
  UserProfileRepository.java
  TransactionRepository.java
  SimulationRecordRepository.java
  ReportRecordRepository.java
```

### Novos DTOs
```
api/dto/
  UserDto.java
  UserProfileDto.java
  TransactionDto.java
  UpdateProfileRequest.java
  GenerateTransactionRequest.java
```

### Novos Services
```
application/service/
  UserService.java
  UserProfileService.java
  TransactionService.java
```

### Novos Controllers
```
api/controller/
  UserController.java        ← /api/user
  UserProfileController.java ← /api/profile
  TransactionController.java ← /api/transaction
  ReportController.java      ← /api/report
```

### Infrastructure
```
infrastructure/persistence/
  DatabaseTransactionProvider.java  ← substitui MockTransactionProvider
```

---

## 📋 SQL Migrations (ddl-auto=none)

Para o ambiente dev/prod com PostgreSQL, criar:
- `db/migrations/V1__add_user_fields.sql` — adiciona `name` e `system_role` a `users`
- `db/migrations/V2__create_simulations.sql` — tabela `simulations`
- `db/migrations/V3__add_transaction_fields.sql` — adiciona `deleted_at`, `is_open_finance` a `transactions`

---

## ✅ Critérios de Aceitação

- [x] GET /api/user retorna dados do usuário autenticado
- [x] PUT /api/user rejeita campos imutáveis com mensagem correta
- [x] GET /api/profile retorna perfil (cria default se não existir)
- [x] PUT /api/profile atualiza report_interval, open_finance, postalCode
- [x] POST /api/transaction/generate cria 1-10 transações aleatórias
- [x] POST /api/transaction/generate rejeita is_open_finance=true sem open_finance ativo
- [x] GET /api/transaction lista transações do usuário (sem deletadas)
- [x] DELETE /api/transaction/{id} faz soft delete
- [x] POST /api/analysis/report retorna comparação de 6 períodos
- [x] GET /api/report/list lista relatórios salvos
- [x] GET /api/report/{id} retorna relatório específico
- [x] GlobalExceptionHandler: email duplicado retorna "E-mail já cadastrado."
- [x] Todos os endpoints retornam formato ApiResponse padronizado

---

## 🧪 Evidências de Teste (CI — H2 in-memory)

```
Tests run: 41, Failures: 0, Errors: 0, Skipped: 0  ✅ BUILD SUCCESS
Data: 2026-05-08

AuthAndUserFlowTest (24 testes — fluxo completo):
  ✅ POST /auth/register → 201 com token JWT
  ✅ POST /auth/register → 400 email duplicado
  ✅ POST /auth/register → 400 senha curta (< 6 chars)
  ✅ POST /auth/login → 200 credenciais válidas
  ✅ POST /auth/login → 401 senha incorreta
  ✅ GET  /api/user → 401 sem token
  ✅ GET  /api/user → 200 com token JWT válido
  ✅ PUT  /api/user → 400 alterar email (imutável)
  ✅ PUT  /api/user → 400 alterar senha (imutável)
  ✅ PUT  /api/user → 200 atualizar systemRole PREMIUM
  ✅ GET  /api/profile → 200 cria perfil padrão (MONTHLY, open_finance=false)
  ✅ PUT  /api/profile → 200 atualizar reportPreference WEEKLY
  ✅ PUT  /api/profile → 400 interval inválido (DIARIO)
  ✅ PUT  /api/profile → 400 holderType (campo imutável)
  ✅ GET  /api/transaction → 200 lista vazia
  ✅ POST /api/transaction/generate → 400 count=0
  ✅ POST /api/transaction/generate → 400 count=11
  ✅ POST /api/transaction/generate → 201 count=3
  ✅ POST /api/transaction/generate?fromOtherBank=true → 400 open finance inativo
  ✅ DELETE /api/transaction/{id} → 200 soft delete
  ✅ DELETE /api/transaction/id-inexistente → 400 não encontrado
  ✅ GET  /api/report/list → 200 lista vazia
  ✅ GET  /api/report/{id} → 200 id inexistente (data=null)
  ✅ POST /api/analysis/report → 200 com header + reportBalance

DefaultCalculationEngineTest (15 testes — motor de cálculo)
FinancialAnalysisControllerTest (2 testes — validação de input)
```

---

**Versão:** 2.0 | **Autor:** Claude (revisão automatizada) | **Data:** 2026-05-08
