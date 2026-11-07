-- =====================================================
-- 02_create_tables.sql
-- Healthcare Claims Tracking & Denial Management System

-- =====================================================

DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS claim_status_history CASCADE;
DROP TABLE IF EXISTS follow_up_tasks CASCADE;
DROP TABLE IF EXISTS denials CASCADE;
DROP TABLE IF EXISTS claims CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

CREATE TABLE roles (
    role_id     SERIAL PRIMARY KEY,
    role_name   VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE users (
    user_id        SERIAL PRIMARY KEY,
    full_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(100) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role_id        INT NOT NULL REFERENCES roles(role_id),
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE claims (
    claim_id         SERIAL PRIMARY KEY,
    claim_reference  VARCHAR(30) NOT NULL UNIQUE,
    patient_ref      VARCHAR(50) NOT NULL,
    claim_amount     NUMERIC(12,2) NOT NULL CHECK (claim_amount > 0),
    submission_date  DATE NOT NULL DEFAULT CURRENT_DATE,
    current_status   VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED'
        CHECK (current_status IN
            ('SUBMITTED','PENDING','PAID','PARTIALLY_PAID','DENIED','APPEALED')),
    created_by       INT REFERENCES users(user_id),
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE denials (
    denial_id          SERIAL PRIMARY KEY,
    claim_id           INT NOT NULL REFERENCES claims(claim_id) ON DELETE CASCADE,
    denial_category    VARCHAR(50) NOT NULL,
    denial_reason      TEXT NOT NULL,
    denial_date        DATE NOT NULL DEFAULT CURRENT_DATE,
    resolution_status  VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (resolution_status IN ('OPEN','IN_PROGRESS','APPEALED','RESOLVED','CLOSED')),
    recorded_by        INT REFERENCES users(user_id),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE follow_up_tasks (
    task_id           SERIAL PRIMARY KEY,
    claim_id          INT NOT NULL REFERENCES claims(claim_id) ON DELETE CASCADE,
    denial_id         INT REFERENCES denials(denial_id) ON DELETE SET NULL,
    assigned_user_id  INT NOT NULL REFERENCES users(user_id),
    title             VARCHAR(150) NOT NULL,
    description       TEXT,
    priority          VARCHAR(10) NOT NULL DEFAULT 'MEDIUM'
        CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    due_date          DATE NOT NULL,
    task_status       VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (task_status IN ('OPEN','IN_PROGRESS','COMPLETED','CANCELLED')),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMP
);

CREATE TABLE claim_status_history (
    history_id       SERIAL PRIMARY KEY,
    claim_id         INT NOT NULL REFERENCES claims(claim_id) ON DELETE CASCADE,
    previous_status  VARCHAR(20),
    new_status       VARCHAR(20) NOT NULL,
    changed_by       INT REFERENCES users(user_id),
    changed_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE payments (
    payment_id      SERIAL PRIMARY KEY,
    claim_id        INT NOT NULL REFERENCES claims(claim_id) ON DELETE CASCADE,
    amount_paid     NUMERIC(12,2) NOT NULL CHECK (amount_paid > 0),
    payment_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    payment_method  VARCHAR(30),
    payment_status  VARCHAR(20) NOT NULL DEFAULT 'PAID'
        CHECK (payment_status IN ('PAID','PARTIALLY_PAID','UNPAID','OVERDUE')),
    recorded_by     INT REFERENCES users(user_id),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    audit_id     SERIAL PRIMARY KEY,
    user_id      INT REFERENCES users(user_id) ON DELETE SET NULL,
    entity_name  VARCHAR(50) NOT NULL,
    record_id    INT NOT NULL,
    action       VARCHAR(20) NOT NULL
        CHECK (action IN ('INSERT','UPDATE','DELETE')),
    details      TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);