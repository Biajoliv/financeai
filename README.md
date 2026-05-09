# FinanceAI

Plataforma de insights financeiros com IA. API REST em Spring Boot que oferece análise de transações, simulação de cenários financeiros (RED/YELLOW/GREEN), relatórios periódicos e geração de dados sintéticos via Open Finance simulado.

## Stack

| Camada | Tecnologia |
|---|---|
| Runtime | Java 21 (Virtual Threads) |
| Framework | Spring Boot 3.3.4 |
| Segurança | Spring Security + JWT |
| Persistência | Spring Data JPA + Hibernate 6 |
| Banco (prod/dev) | PostgreSQL 16 + pgvector |
| Banco (testes) | H2 in-memory |
| Build | Maven 3.9 |
| Containers | Docker + Docker Compose |
| Docs | SpringDoc OpenAPI (Swagger UI) |

---

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker e Docker Compose (para execução containerizada)
- PostgreSQL 16+ (para execução local sem Docker)

---

## Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto com as variáveis abaixo.

```dotenv
# ── Banco de Dados ────────────────────────────────────────────────────────────
# URL JDBC completa (inclua sslmode=require para bancos em nuvem)
DATABASE_URL=jdbc:postgresql://localhost:5434/financeai?stringtype=unspecified

DATABASE_USERNAME=financeai_user
DATABASE_PASSWORD=sua_senha_aqui

# ── PostgreSQL (usado pelo Docker Compose) ───────────────────────────────────
POSTGRES_DB=financeai
POSTGRES_USER=financeai_user
POSTGRES_PASSWORD=sua_senha_aqui

# ── Segurança ─────────────────────────────────────────────────────────────────
# Chave HMAC para assinar tokens JWT — mínimo 32 caracteres
JWT_SECRET=sua-chave-jwt-super-segura-minimo-32-chars

# ── IA (OpenRouter) ───────────────────────────────────────────────────────────
# Obtenha em https://openrouter.ai — use "disabled" para desativar
OPENAI_API_KEY=sk-or-sua_chave_aqui

# ── Perfil Spring ─────────────────────────────────────────────────────────────
# Valores: dev | docker | prod
SPRING_PROFILES_ACTIVE=dev
```

### Perfis disponíveis

| Perfil | Banco | DDL | Uso |
|---|---|---|---|
| `dev` | PostgreSQL (externo) | `none` | Desenvolvimento local |
| `docker` | PostgreSQL (container) | `update` | Docker Compose local |
| `prod` | PostgreSQL (nuvem) | `update` | Render / Railway / Fly.io |
| `test` | H2 in-memory | `create-drop` | Testes automatizados |

> **Atenção:** Os perfis `dev` e `prod` usam `ddl-auto=none` — o schema deve ser criado manualmente via migrations (veja [Migrations](#migrations)).

---

## Build

```bash
# Compilar e empacotar (sem rodar testes)
mvn clean package -DskipTests

# Compilar e rodar todos os testes
mvn clean verify

# Apenas compilar (sem gerar JAR)
mvn compile
```

O JAR gerado fica em `target/financeai-0.0.1-SNAPSHOT.jar`.

---

## Execução

### 1. Local (Maven)

Requer PostgreSQL acessível e `.env` configurado na raiz do projeto.

```bash
mvn spring-boot:run
```

### 2. Docker Compose

Sobe a aplicação + PostgreSQL com pgvector em containers isolados.

```bash
# Subir tudo
docker compose up --build

# Subir em background
docker compose up --build -d

# Derrubar e remover volumes
docker compose down -v
```

A aplicação ficará disponível em `http://localhost:8080`.  
O PostgreSQL estará exposto na porta `5434` do host.

### 3. JAR direto

```bash
java -jar target/financeai-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --DATABASE_URL=jdbc:postgresql://... \
  --DATABASE_USERNAME=... \
  --DATABASE_PASSWORD=... \
  --JWT_SECRET=...
```

---

## Migrations

O projeto **não usa Flyway nem Liquibase**. As migrations SQL ficam em `src/main/resources/db/migrations/` e devem ser executadas manualmente na ordem indicada pelo prefixo `V`.

```
V1__add_user_fields.sql
V2__add_transaction_fields.sql
V3__create_simulations_and_reports.sql
V4__fix_transactions_id_type.sql
```

Para aplicar no banco (exemplo com psql):

```bash
psql "$DATABASE_URL" -f src/main/resources/db/migrations/V1__add_user_fields.sql
psql "$DATABASE_URL" -f src/main/resources/db/migrations/V2__add_transaction_fields.sql
psql "$DATABASE_URL" -f src/main/resources/db/migrations/V3__create_simulations_and_reports.sql
psql "$DATABASE_URL" -f src/main/resources/db/migrations/V4__fix_transactions_id_type.sql
```

> O `init.sql` na raiz é executado automaticamente pelo Docker Compose na primeira inicialização do container PostgreSQL.

---

## Testes

```bash
# Rodar todos os testes (usa H2 in-memory, não precisa de banco)
mvn test

# Rodar uma classe específica
mvn test -Dtest=AuthAndUserFlowTest

# Pular testes no build
mvn package -DskipTests
```

Os testes usam o perfil `test` automaticamente via `@ActiveProfiles("test")`.

---

## Documentação da API

Com a aplicação rodando, acesse:

- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **Status do motor:** `GET /api/analysis/status`

### Endpoints principais

```
POST   /auth/register                   Cadastrar usuário
POST   /auth/login                      Login (retorna JWT)
DELETE /auth/account                    Deletar conta

GET    /api/transaction                 Listar transações
POST   /api/transaction/generate        Gerar transações sintéticas
DELETE /api/transaction/{id}            Remover transação

POST   /api/analysis/simulate           Simular cenários RED/YELLOW/GREEN
POST   /api/analysis/report             Gerar relatório periódico
GET    /api/analysis/report/periods     Períodos disponíveis (WEEKLY/BIWEEKLY/MONTHLY)
GET    /api/analysis/simulation/list    Listar simulações salvas
GET    /api/analysis/simulation/{id}    Detalhe de uma simulação

GET    /api/user                        Dados do usuário autenticado
PUT    /api/user                        Atualizar systemRole (FREE/PREMIUM)
GET    /api/profile                     Perfil do usuário
PUT    /api/profile                     Atualizar perfil
```

Todas as rotas (exceto `/auth/register` e `/auth/login`) exigem o header:

```
Authorization: Bearer <token>
```

---

## Estrutura de Pacotes

```
src/main/java/com/financeai/
├── api/
│   ├── controller/     Controladores REST
│   └── dto/            Request/Response DTOs
├── application/
│   ├── service/        Serviços de aplicação
│   └── usecase/        Casos de uso (orquestração)
├── domain/
│   ├── entity/         Entidades JPA
│   ├── model/          Modelos de domínio (records)
│   └── port/           Interfaces (portas da arquitetura hexagonal)
├── infrastructure/
│   ├── persistence/    Repositórios Spring Data
│   ├── security/       JWT, filtros, configuração Spring Security
│   └── simulation/     Adaptadores: motor de cálculo, providers
└── config/             Beans de configuração (DotenvConfig, etc.)
```
