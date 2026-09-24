# Contrato da API

Base: `/api/v1`. JSON em tudo. Autenticação por `Authorization: Bearer <jwt>`.
Swagger UI em `/api/v1/swagger-ui.html`, OpenAPI em `/api/v1/docs`. Saúde em `/actuator/health`.

Erros saem como [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) (`application/problem+json`):

```json
{ "status": 400, "title": "Bad Request", "detail": "Validation failed", "errors": { "amount": "must be greater than or equal to 0.01" } }
```

| Status | Quando |
|---|---|
| 400 | Validação de campo, enum desconhecido, JSON malformado |
| 401 | Sem token, token inválido/expirado, usuário desativado, credenciais erradas |
| 403 | Autenticado, mas o papel ou a posse não permitem a ação |
| 404 | Recurso não existe **ou não é visível** para quem pediu (não vaza existência) |
| 409 | Email/username duplicado, transição de estado inválida, edição concorrente (`@Version`) |
| 422 | Regra de negócio: submeter sem lançamentos, rejeitar sem nota |

## Papéis

| Papel | Pode |
|---|---|
| `ANALYST` | Padrão do cadastro. Cria relatórios, edita os próprios em `DRAFT`/`REJECTED`, submete |
| `USER` | Leitura (viewer). Rebaixado por um admin |
| `ADMIN` | Vê tudo, aprova/rejeita (nunca o próprio), exclui qualquer relatório, gerencia usuários, lê a auditoria |

O primeiro admin vem de `ADMIN_EMAIL`/`ADMIN_PASSWORD`. O papel nunca vem do JSON do cliente, e o JWT carrega só o id: papel e `active` são relidos do banco a cada request.

**Visibilidade**: não-admin vê os próprios relatórios e os `APPROVED` de qualquer um. Vale para lista, detalhe, lançamentos e analytics.

## Auth

| Método | Caminho | Corpo | Resposta |
|---|---|---|---|
| POST | `/auth/register` | `{username, email, fullName, password}` | 201 `TokenResponse` |
| POST | `/auth/login` | `{email, password}` | 200 `TokenResponse` |
| GET | `/auth/me` | | `UserResponse` |

`TokenResponse = {token, type: "Bearer", expiresIn (s), user: UserResponse}`
`UserResponse = {id, username, email, fullName, role, active}`

Email e username são únicos sem diferenciar maiúsculas (índice em `lower()`). Senha: 8 a 72 caracteres (limite do bcrypt).

## Relatórios

| Método | Caminho | Quem |
|---|---|---|
| GET | `/reports?status=&riskLevel=&q=&mine=&page=&size=` | todos (filtrado pela visibilidade) |
| POST | `/reports` `{title, period, description?}` | ANALYST, ADMIN |
| GET | `/reports/{id}` | visível |
| PUT | `/reports/{id}` `{title, period, description?}` | dono, em DRAFT/REJECTED (volta para DRAFT) |
| DELETE | `/reports/{id}` | dono em DRAFT/REJECTED, ou ADMIN |
| POST | `/reports/{id}/records` `{recordDate, type, category, amount, description?}` | dono, editável |
| DELETE | `/reports/{id}/records/{recordId}` | dono, editável |
| POST | `/reports/{id}/submit` | dono, editável, com ≥1 lançamento |
| POST | `/reports/{id}/approve` `{note?}` | ADMIN, não dono, status PENDING |
| POST | `/reports/{id}/reject` `{note}` | ADMIN, não dono, status PENDING, nota obrigatória |

`period`: `YYYY-MM` ou `YYYY-Q1..Q4`. `amount`: > 0, até 13 dígitos inteiros e 2 decimais (`NUMERIC(15,2)`, `BigDecimal` ponta a ponta). `size` máximo 100.

Lista devolve `{content: ReportSummary[], page, size, totalElements, totalPages}`, ordenada por `updatedAt desc`.
Detalhe devolve também `records`, `riskFactors`, `reviewNote` e `can: {edit, submit, review, delete}`, que o frontend usa só para esconder botões: o servidor valida de novo.

Totais, `riskScore`, `riskLevel` e dono **nunca** vêm do payload; são derivados dos lançamentos a cada mudança.

### Estados

```
DRAFT ──submit──▶ PENDING ──approve──▶ APPROVED (final, só leitura)
  ▲                  │
  └──edit── REJECTED ◀┘ reject (com nota)
```

## Score de risco

Função pura em `RiskCalculator`, recalculada em toda inclusão/remoção de lançamento. `ratio = despesa / receita`.

| Fator | Pontos | Condição |
|---|---|---|
| `NO_INCOME` | 70 | despesa > 0 e receita = 0 (sem divisão por zero) |
| `DEFICIT` | 40 | ratio > 1,00 |
| `THIN_MARGIN` | 20 | 0,80 < ratio ≤ 1,00 |
| `SEVERE_DEFICIT` | +20 | ratio > 2,00 (soma com `DEFICIT`) |
| `CONCENTRATION` | 15 | uma categoria de despesa (sem diferenciar caixa/espaços) > 50% da despesa |

Score = soma, limitado a 100. Sem lançamentos ou sem despesa: 0.
Níveis: `LOW` 0–24, `MEDIUM` 25–49, `HIGH` 50–74, `CRITICAL` 75–100.

## Analytics

`GET /analytics/summary` sobre os relatórios visíveis:
`{totalReports, totalIncome, totalExpense, averageRiskScore, byStatus, byRiskLevel, byPeriod: [{period, income, expense, averageRiskScore}], topRisks: ReportSummary[5]}`.
`byPeriod` sai em ordem cronológica (`2026-Q1` começa em janeiro).

## Admin (`ROLE_ADMIN`)

| Método | Caminho | |
|---|---|---|
| GET | `/admin/users` | lista |
| PATCH | `/admin/users/{id}` `{role?, active?}` | 403 se for você mesmo |
| GET | `/admin/audit-logs?page=&size=` | mais recente primeiro |

A auditoria é gravada pelo servidor na mesma transação da mudança (`Propagation.MANDATORY`): não existe mudança sem log nem log sem mudança, e não há endpoint para escrever nela.
