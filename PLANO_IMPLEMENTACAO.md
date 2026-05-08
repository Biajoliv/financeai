# Plano de Melhorias do FinanceAI - Implementação Abrangente

## 📋 Declaração do Problema
O MVP do FinanceAI (Java Spring Boot + PostgreSQL) necessita de melhorias críticas para entrega no Hackathon:
1. **Operações CRUD de usuário** - Gerenciamento incompleto de dados do usuário
2. **Arquitetura orientada a banco de dados** - Remover dados mockados das rotas
3. **Formatos de resposta apropriados** - Padronizar respostas da API com estrutura consistente
4. **Geração de transações** - Adicionar rota para criar transações aleatórias com validação de Open Finance
5. **Relatórios avançados** - Implementar relatórios com comparação de 6 meses com armazenamento por período
6. **Persistência de dados** - Armazenar simulações e relatórios no banco com rotas de recuperação

---

## ⚠️ Restrições Críticas (NÃO NEGOCIÁVEIS)
- ⚠️ **NÃO modificar arquivos de Auth/JWT/Security** - Login é sensível
- ⚠️ **NÃO mudar .env ou configs de infraestrutura** - Evitar conflitos de branch
- ⚠️ **Manter login com APENAS email + senha** - Não quebrar compilação
- ✅ **Apenas soft delete** (deleted_at != null) - Sem deleções permanentes
- ✅ **Regras HTTP** + códigos de status apropriados em todas as respostas
- ⚠️ **CAMPOS IMUTÁVEIS**: email, senha, documento (CPF/CNPJ), account_holder_type
- ✅ **system_role**: apenas 'FREE' ou 'PREMIUM' (padrão: 'FREE')
- ✅ **report_interval**: padrão 'MONTHLY', **MUTÁVEL** via PUT /profile
- ✅ **Períodos suportados**: WEEKLY (semanal), BIWEEKLY (quinzenal), MONTHLY (mensal)

---

## 🎯 Abordagem de Implementação

### 7 Fases Sequenciais:
1. **Fase 1:** Análise & Setup (preparar banco de dados + entidades)
2. **Fase 2:** Gerenciamento de Usuário e Perfil (CRUD completo + update imutáveis)
3. **Fase 3:** Gerenciamento de Transações (geração + persistência)
4. **Fase 4:** Relatórios e Simulações (armazenamento + recuperação)
5. **Fase 5:** Padronização de Formato de Resposta
6. **Fase 6:** Testes & Validação
7. **Fase 7:** Documentação

---

## ✅ Lista de Tarefas (42 tarefas)

### FASE 1: Setup de Banco de Dados (5 tarefas)

- [ ] **task-1-db-schema** 
  - Analisar schema atual e planejar migração da entidade UserProfile com todos os campos

- [ ] **task-2-entity-create** 
  - Criar entidade JPA UserProfile com: cpf_cnpj, endereco, aniversario, open_finance, report_interval (padrão MONTHLY), account_holder_type, system_role (padrão FREE)

- [ ] **task-3-repo-create** 
  - Criar UserProfileRepository, TransactionRepository, SimulationRepository, ReportRepository, CategoryWeightRepository

- [ ] **task-4-migrations** 
  - Garantir que migrations criem tabelas: user_profiles, simulations, reports, category_weights

- [ ] **task-5-enums** 
  - Criar enums: ReportInterval (WEEKLY, BIWEEKLY, MONTHLY), SystemRole (FREE, PREMIUM)

---

### FASE 2: CRUD de Usuário e Perfil (8 tarefas)

- [ ] **task-6-user-service** 
  - Criar UserService com operações de leitura e atualização (GET, PUT - apenas campos mutáveis)

- [ ] **task-7-profile-service** 
  - Criar UserProfileService para gerenciar dados de configuração do usuário

- [ ] **task-8-user-controller** 
  - Criar UserController com endpoints GET /user, PUT /user, DELETE /account (protegido)

- [ ] **task-9-profile-controller** 
  - Criar UserProfileController com GET /profile, PUT /profile (protegido)

- [ ] **task-10-register-flow** 
  - Modificar fluxo de registro para criar User + UserProfile padrão (report_interval=MONTHLY, system_role=FREE)

- [ ] **task-11-immutable-validation** 
  - Adicionar validação: campos imutáveis (email, senha, documento, account_holder_type) não podem ser alterados via PUT

- [ ] **task-12-system-role-validation** 
  - Validar system_role apenas 'FREE' ou 'PREMIUM'

- [ ] **task-13-update-report-interval** 
  - Implementar PUT /profile para permitir alteração de report_interval entre WEEKLY, BIWEEKLY, MONTHLY

---

### FASE 3: Gerenciamento de Transações (7 tarefas)

- [ ] **task-14-transaction-repo** 
  - Aprimorar TransactionRepository com filtros (por usuário, intervalo de datas, categoria, excluir deleted_at != null)

- [ ] **task-15-transaction-service** 
  - Criar TransactionService com CRUD + lógica de geração aleatória respeitando pesos de categoria

- [ ] **task-16-transaction-controller** 
  - Criar TransactionController com POST /api/transaction/generate (1-10 transações aleatórias, máx 10)

- [ ] **task-17-open-finance-validation** 
  - Validação: só permitir transações de outros bancos se user.open_finance=true, retornar erro seguro se não permitido

- [ ] **task-18-transaction-weights** 
  - Carregar pesos de categoria do banco de dados (não hardcoded em Enum)

- [ ] **task-19-mock-removal** 
  - Remover dependência MockIngestionService de SimulateScenariosUseCase

- [ ] **task-20-tx-generation-logic** 
  - Implementar geração realista de transações:
    - Respeitar perfil do usuário (PF vs tipo de PJ)
    - Aplicar pesos de categoria para cálculo do knapsack
    - Gerar 1-10 transações aleatórias por requisição com máximo de 10
    - Validar que valores não quebram lógica do knapsack

---

### FASE 4: Armazenamento de Relatórios e Simulações (8 tarefas)

- [ ] **task-21-simulation-entity** 
  - Criar entidade Simulation com: user_id, goal_data (JSON), scenario_results (JSON), created_at

- [ ] **task-22-report-entity** 
  - Criar entidade Report com: user_id, interval (WEEKLY/BIWEEKLY/MONTHLY), header (JSON), report_data (JSON), comparison (JSON), alerts (JSON), created_at

- [ ] **task-23-simulation-repo** 
  - Criar SimulationRepository com filtros por usuário e intervalo de datas

- [ ] **task-24-report-repo** 
  - Criar ReportRepository com filtros por usuário, intervalo e intervalo de datas

- [ ] **task-25-simulation-service** 
  - Atualizar SimulateScenariosUseCase para persistir resultados de simulação no banco

- [ ] **task-26-report-service** 
  - Atualizar ReportUseCase para:
    - Gerar comparação de 6 períodos para relatórios (semanal/quinzenal/mensal)
    - Persistir relatório com estrutura: header, reportBalance, comparison, alerts
    - Suportar períodos WEEKLY, BIWEEKLY, MONTHLY

- [ ] **task-27-simulation-controller** 
  - Criar endpoints: POST /api/simulation (cria + armazena), GET /api/simulation/list, GET /api/simulation/{id}

- [ ] **task-28-report-controller** 
  - Criar endpoints: GET /api/report/list, GET /api/report/{id}, suportar filtro por interval

---

### FASE 5: Padronização de Formato de Resposta (5 tarefas)

- [ ] **task-29-error-handler** 
  - Criar GlobalExceptionHandler com respostas padronizadas de erro:
    - Não expor nomes de arquivo ou stack traces
    - Estrutura de erro: { "message": "...", "timestamp": "..." }
    - Erro seguro para email duplicado: "E-mail já cadastrado." (sem expor email)

- [ ] **task-30-success-response** 
  - Padronizar todos os endpoints para retornar formato de sucesso:
    - Estrutura: { "message": "...", "data": {...}, "timestamp": "..." }
    - Mesmo queries vazias retornam HTTP 200 com message: "Não foram encontrados dados."
    - Códigos de status HTTP seguem regras REST (200, 201, 400, 401, 404, 500)

- [ ] **task-31-dto-updates** 
  - Criar DTOs: UserDto, UserProfileDto, TransactionDto, SimulationDto, ReportDto

- [ ] **task-32-controller-updates** 
  - Atualizar todos os controllers (Auth, Account, Analysis, User, Profile, Transaction, Simulation, Report) para usar respostas padronizadas

- [ ] **task-33-deleted-at-filter** 
  - Adicionar filtro automático em todos os @Query para excluir deleted_at != null

---

### FASE 6: Testes & Validação (7 tarefas)

- [ ] **task-34-user-crud-tests** 
  - Testar: criar usuário, ler usuário, atualizar perfil, deletar usuário (soft delete verificando deleted_at)

- [ ] **task-35-immutable-fields-test** 
  - Verificar que email, senha, documento não podem ser alterados; system_role apenas FREE/PREMIUM; report_interval pode ser alterado

- [ ] **task-36-transaction-tests** 
  - Testar: gerar transações aleatórias, validar requisito Open Finance, verificar que máximo é 10 transações, valores realistas

- [ ] **task-37-report-tests** 
  - Testar: gerar relatórios com diferentes períodos (WEEKLY/BIWEEKLY/MONTHLY)
    - Verificar cálculo de comparação de 6 períodos
    - Testar com usuários PF e PJ
    - Verificar persistência no banco

- [ ] **task-38-simulation-tests** 
  - Testar: criar simulação, recuperar por ID, listar simulações, verificar persistência

- [ ] **task-39-response-format-tests** 
  - Verificar que todos os endpoints retornam formato correto (sucesso/erro), timestamps válidos

- [ ] **task-40-edge-cases** 
  - Testar: perfis ausentes, sem transações, sem relatórios, emails duplicados, acesso de usuário deletado, campos imutáveis

---

### FASE 7: Documentação (2 tarefas)

- [ ] **task-41-api-documentation** 
  - Criar documento api_routes.txt com:
    - Todos os endpoints (marcadores NEW/UPDATE/EXISTING)
    - Método HTTP, endpoint, descrição
    - Formato do body da requisição (campos obrigatórios marcados com *)
    - Formato de resposta esperada
    - Apenas /auth/register, /auth/login, /api/analysis/status públicos; outros requerem JWT

- [ ] **task-42-test-evidence** 
  - Criar test_evidence.txt com:
    - Todos os cenários de teste executados com timestamps
    - Resultados de queries SQL
    - Dados de teste criados (usuários, transações, relatórios, simulações)
    - Logs de execução

---

## 📊 Detalhes Críticos de Implementação

### Entidades JPA Necessárias:
1. **User** (existente) - Adicionar relacionamento com UserProfile
2. **UserProfile** (nova) - cpf_cnpj, endereco, aniversario, open_finance, report_interval (padrão MONTHLY), account_holder_type, system_role (padrão FREE)
3. **Transaction** (existente) - Aprimorar repositório
4. **Simulation** (nova) - Armazenar resultados de cálculo
5. **Report** (nova) - Armazenar relatórios periódicos com comparação de 6 períodos
6. **CategoryWeight** (nova) - Pesos de categoria carregados do banco

---

### 🔐 Rotas Públicas (sem JWT):
- `POST /auth/register` - Criar usuário + perfil padrão
  Obrigatórios: nome, cpf_cnpj, email, senha
- `POST /auth/login` - Obter token JWT
  Obrigatórios: email, senha
- `GET /api/analysis/status` - Status operacional

### 🔒 Rotas Protegidas (requerem JWT):

**Usuário:**
- `GET /auth/user` - Obter usuário atual
- `PUT /auth/user` - Atualizar usuário (apenas campos mutáveis)
- `DELETE /auth/account` - Deletar conta (soft delete)

**Perfil:**
- `GET /auth/profile` - Obter perfil do usuário
- `PUT /auth/profile` - Atualizar perfil (pode alterar report_interval, não pode alterar campos imutáveis)

**Transações:**
- `POST /api/transaction/generate` - Gerar transações aleatórias (1-10, máx 10)
- `GET /api/transaction` - Listar transações do usuário
- `DELETE /api/transaction/{id}` - Deletar transação (soft delete)

**Relatórios:**
- `GET /api/report/list` - Listar relatórios do usuário
- `GET /api/report/{id}` - Obter relatório específico

**Análise (existentes, refatorados):**
- `POST /api/analysis/simulate` - Executar simulação (usa dados do banco, não mocked)
  (enviado pela IA)
  Obrigatório: "goal": {
        "name": "O que o usuário quer?",
        "targetAmount": ,
        "deadline": "YYYY-MM-DD",
        "profile": "PF ou PJ"
    }
- `GET /api/analysis/simulation/list` - Listar simulações do usuário
- `GET /api/analysis/simulation/{id}` - Obter simulação específica
- `GET /api/analysis/config/weights` - Obter pesos de categoria do banco
- `GET /api/analysis/config/tax-rules` - Obter regras de tributação
- `DELETE /api/analysis/{diagnosticId}` - Deletar diagnóstico

---

## 📐 Formatos de Resposta

### ✅ Sucesso:
```json
{
  "message": "Simulação processada com sucesso.",
  "data": { /* payload de resposta */ },
  "timestamp": "2026-05-08T16:46:24Z"
}
```

### ❌ Erro:
```json
{
  "message": "E-mail já cadastrado.",
  "timestamp": "2026-05-08T16:46:24Z"
}
```

### 📭 Sem dados:
```json
{
  "message": "Não foram encontrados dados.",
  "data": null,
  "timestamp": "2026-05-08T16:46:24Z"
}
```

### 📊 Estrutura de Relatório (comparação 6 períodos):
```json
{
  "message": "Relatório gerado com sucesso.",
  "data": {
    "header": {
      "title": "Relatório Mensal",
      "subtitle": "Período: Jan-Jun 2026",
      "name": "João Silva"
    },
    "report": {
      "reportBalance": 1594.10,
      "comparison": {
        "Housing": [2000, 2050, 2100, 2150, 2200, 2250],
        "Entertainment": [500, 520, 540, 560, 580, 600]
      },
      "alerts": [
        "Risco de liquidez detectado no curto prazo."
      ]
    }
  },
  "timestamp": "2026-05-08T16:46:24Z"
}
```

---

## 🔒 Validação de Campos Imutáveis

**Não podem ser alterados:**
- **email** → Error: "E-mail não pode ser alterado."
- **password** → Error: "Senha não pode ser alterada."
- **documento (CPF/CNPJ)** → Error: "Documento não pode ser alterado."
- **account_holder_type** → Error: "Tipo de titular não pode ser alterado."

**Podem ser alterados via PUT /profile:**
- **report_interval**: WEEKLY, BIWEEKLY, MONTHLY
- **open_finance**: true/false
- **endereco**: string (endereço)

---

## 🧪 Estratégia de Testes
- **Testes Unitários:** Services, repositories, cálculos
- **Testes de Integração:** Controllers, persistência no banco
- **Testes E2E:** Fluxos completos (registrar → transação → relatório → simulação)
- **QA Manual:** 
  - Usuários PF com/sem Open Finance
  - Usuários PJ com diferentes tipos
  - Diferentes períodos de relatório (WEEKLY/BIWEEKLY/MONTHLY)
  - Verificação de soft delete

---

## ✨ Critérios de Sucesso

✅ Todas as rotas funcionando com dados persistidos no banco
✅ Formato de resposta JSON padronizado (sucesso/erro)
✅ Soft delete funcionando (filtro deleted_at)
✅ CRUD completo de usuário com dados de perfil separados
✅ Comparação de relatório funcionando para todos os períodos
✅ Geração de transações com validação de Open Finance
✅ Simulações e relatórios recuperáveis via list/get
✅ Sem dados hardcoded (todos do banco ou calculados)
✅ Todos os testes passando
✅ Documentação completa e clara
✅ Campos imutáveis protegidos
✅ system_role apenas FREE/PREMIUM
✅ report_interval mutável via PUT /profile

---

## 📝 Resumo de Arquivos

### Novos Arquivos (~18-22):
- **Entidades:** UserProfile, Simulation, Report, CategoryWeight
- **Repositórios:** UserProfileRepository, TransactionRepository, SimulationRepository, ReportRepository, CategoryWeightRepository
- **Serviços:** UserService, UserProfileService, TransactionService, SimulationService (updated), ReportService (updated)
- **Controllers:** UserController, UserProfileController, TransactionController, SimulationController, ReportController
- **DTOs:** UserDto, UserProfileDto, TransactionDto, SimulationDto, ReportDto
- **Exception Handler:** GlobalExceptionHandler
- **Testes:** ~7 test classes para cobertura completa

### Arquivos a Modificar (~8-10):
- **User** entity - Adicionar relacionamento com UserProfile
- **AuthController/AuthUseCase** - Modificar fluxo de registro
- **FinancialAnalysisController** - Refatorar para usar dados do banco
- **SimulateScenariosUseCase** - Remover mock, adicionar persistência
- **ReportUseCase** - Adicionar lógica de 6-month comparison + persistência
- **pom.xml** - Se novos dependencies forem necessários

---

## 🚀 Como Começar

1. **Checkout da branch:** `git checkout feature/improvements`
2. **Comece pela FASE 1:** Analisar schema e criar entidades
3. **Faça commits pequenos e frequentes:** `git commit -m "feat: [task-id] descrição"`
4. **Push regularmente:** `git push origin feature/improvements`
5. **Documente seu progresso:** Atualize este arquivo marcando tarefas como completas

---

**Última atualização:** 2026-05-08
**Status:** Pronto para implementação ✨
