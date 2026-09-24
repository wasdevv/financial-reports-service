CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(100) NOT NULL,
    password    VARCHAR(100) NOT NULL,
    full_name   VARCHAR(120) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'ANALYST' CHECK (role IN ('ADMIN', 'ANALYST', 'USER')),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- Unicidade case-insensitive: "Ana@x.com" e "ana@x.com" são a mesma conta.
CREATE UNIQUE INDEX uq_users_email ON users (lower(email));
CREATE UNIQUE INDEX uq_users_username ON users (lower(username));

CREATE TABLE reports (
    id             BIGSERIAL PRIMARY KEY,
    owner_id       BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title          VARCHAR(160)  NOT NULL,
    description    TEXT,
    period         VARCHAR(7)    NOT NULL CHECK (period ~ '^[0-9]{4}-(0[1-9]|1[0-2]|Q[1-4])$'),
    status         VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED')),
    total_income   NUMERIC(15,2) NOT NULL DEFAULT 0,
    total_expense  NUMERIC(15,2) NOT NULL DEFAULT 0,
    risk_score     INTEGER       NOT NULL DEFAULT 0 CHECK (risk_score BETWEEN 0 AND 100),
    risk_level     VARCHAR(20)   NOT NULL DEFAULT 'LOW' CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    review_note    TEXT,
    version        BIGINT        NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_reports_owner ON reports (owner_id, created_at DESC);
CREATE INDEX idx_reports_status ON reports (status);

CREATE TABLE financial_records (
    id           BIGSERIAL PRIMARY KEY,
    report_id    BIGINT        NOT NULL REFERENCES reports (id) ON DELETE CASCADE,
    record_date  DATE          NOT NULL,
    type         VARCHAR(10)   NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    category     VARCHAR(60)   NOT NULL,
    amount       NUMERIC(15,2) NOT NULL CHECK (amount > 0),
    description  VARCHAR(255),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_financial_records_report ON financial_records (report_id, record_date);

CREATE TABLE risk_assessments (
    id           BIGSERIAL PRIMARY KEY,
    report_id    BIGINT       NOT NULL REFERENCES reports (id) ON DELETE CASCADE,
    factor       VARCHAR(40)  NOT NULL,
    points       INTEGER      NOT NULL CHECK (points > 0),
    explanation  VARCHAR(255) NOT NULL
);
CREATE INDEX idx_risk_assessments_report ON risk_assessments (report_id);

-- Append-only: sem FK para reports, a trilha sobrevive à exclusão do relatório.
CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    actor_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    action       VARCHAR(40)  NOT NULL,
    entity_type  VARCHAR(30)  NOT NULL,
    entity_id    BIGINT,
    details      VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at DESC);
