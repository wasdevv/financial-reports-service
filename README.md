# Financial Reports Service

Gestão de relatórios financeiros com análise de risco. Cada relatório é um conjunto de lançamentos de receita e despesa. O servidor deriva os totais e um **score de risco de 0 a 100 com os fatores que o explicam**, e o relatório passa por um fluxo de aprovação com trilha de auditoria.

**Stack:** Java 17 · Spring Boot 3.5 · Spring Security + JWT · Spring Data JPA · PostgreSQL 15 · Flyway · springdoc-openapi · React 19 · Vite · Tailwind CSS 4 · JUnit 5 · Testcontainers · JaCoCo · Docker · GitHub Actions

```
frontend/  React + Vite (Vercel)  ──fetch /api/v1 (Bearer JWT)──▶  backend/  Spring Boot REST
                                                                        │  JPA / Hibernate
                                                                        ▼
                                                           PostgreSQL (schema versionado pelo Flyway)
```

## O que tem dentro

- **Risco calculado no servidor.** Totais, score e nível nunca vêm do cliente: são derivados dos lançamentos a cada inclusão ou remoção. A regra é uma função pura (`RiskCalculator`) com limiares documentados e testados nas bordas (80%, 100%, 200%, receita zero).
- **Fluxo `DRAFT → PENDING → APPROVED | REJECTED`**. Só o dono edita, e só em rascunho ou rejeitado. Rejeição exige nota, e nenhum admin aprova o próprio relatório.
- **Autorização por posse, não só por papel.** Relatório que você não pode ver responde 404 (não vaza que o id existe); visível mas sem permissão, 403.
- **JWT com só o id do usuário.** Papel e status são relidos do banco a cada request, então rebaixar ou desativar alguém vale na hora.
- **Auditoria transacional.** O log é gravado na mesma transação da mudança (`Propagation.MANDATORY`), e não existe endpoint para escrever nele.
- **Dinheiro em `BigDecimal`/`NUMERIC(15,2)`** ponta a ponta, concorrência otimista com `@Version` (409 em vez de sobrescrever).
- **Erros em RFC 9457** (`application/problem+json`) com erros por campo.
- **Frontend responsivo** (320px a desktop, claro e escuro): dashboard com gráfico de receita x despesa e distribuição de risco, lista com filtros e paginação, detalhe com lançamentos e explicação do score, fila de revisão, gestão de papéis e trilha de auditoria.

Contrato completo, papéis, estados e regra de score: [`docs/api.md`](docs/api.md).

## Rodando localmente

Pré-requisitos: **Java 17+**, **Node 20.19+ / 22+** e **Docker**. Maven não precisa estar instalado, porque o `backend/mvnw` baixa a versão fixada.

```bash
# Postgres + API em http://localhost:8080 (build da imagem incluído)
docker compose up --build --wait

# Frontend em http://localhost:5173
cd frontend && cp .env.example .env && npm install && npm run dev
```

O compose sobe a API com `DEMO_SEED=true` (relatórios de exemplo, de um dono com senha aleatória) e um admin de desenvolvimento: `admin@local.dev` / `local-admin-password`. Crie sua conta pela tela de cadastro para entrar como analista.

Portas ocupadas? `POSTGRES_PORT=55432 API_PORT=18080 docker compose up --build --wait` e aponte `VITE_API_URL` para a nova porta.

Para rodar a API fora do Docker: `cd backend && JWT_SECRET=$(openssl rand -base64 48) ./mvnw spring-boot:run` com um Postgres em `localhost:5432` (veja `.env.example`).

## Testes

```bash
cd backend && ./mvnw verify           # 28 testes; falha se a cobertura de linha cair abaixo de 85%
cd frontend && npm test -- --run      # cliente HTTP: token, 401 derruba a sessão, erros por campo
```

Os testes de integração sobem a aplicação inteira contra **PostgreSQL real via Testcontainers**, com o schema criado pela **mesma migration Flyway** de produção. Login de verdade (bcrypt + JWT assinado), sem mock de service nem de repositório. Cobrem cadastro (papel no JSON ignorado, duplicidade case-insensitive), tokens inválidos e adulterados, desativação derrubando token já emitido, isolamento entre usuários, recálculo de risco, o fluxo inteiro de aprovação, validação de valores e CORS.

Cobertura medida pelo JaCoCo na última execução: **86% das linhas** (relatório em `backend/target/site/jacoco/index.html`).

A CI (`.github/workflows/ci.yml`) roda as duas suítes e o build de produção do frontend a cada push e PR.

## Configuração

Tudo vem do ambiente (`.env.example` lista as variáveis):

| Variável | Obrigatória | |
|---|---|---|
| `DATABASE_URL` | sim | JDBC, ex. `jdbc:postgresql://host:5432/db` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | sim | |
| `JWT_SECRET` | sim | ≥ 32 bytes; sem ele o boot falha |
| `CORS_ORIGINS` | sim em produção | origens exatas, separadas por vírgula |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | não | cria ou promove o admin inicial (senha ≥ 12) |
| `DEMO_SEED` | não | `true` popula exemplos se não houver relatórios |
| `JWT_EXPIRATION` | não | ISO-8601, padrão `PT8H` |

## Decisões

- **Cadastro cria `ANALYST`**, não `USER`: numa demo pública, quem se cadastra precisa conseguir criar um relatório. `USER` (somente leitura) continua existindo e é atribuído por um admin.
- **Sem trigger de auditoria nem busca full-text.** A auditoria fica na aplicação para registrar o *ator* autenticado, coisa que um trigger não sabe. A busca é `ILIKE` no título, o bastante para o volume de um relatório por período.
- **Analytics agrega em memória** os relatórios visíveis. Com dezenas de milhares por usuário, a troca é por `GROUP BY` no banco.
- **Gráficos sem biblioteca**: barras em CSS com tooltip, legenda e visão em tabela, cores validadas para daltonismo.

## Licença

MIT
