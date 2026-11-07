-- =====================================================
-- 04_indexes.sql
-- Indexes for frequently searched / joined columns
-- =====================================================

-- Claims
CREATE INDEX idx_claims_status          ON claims(current_status);
CREATE INDEX idx_claims_submission_date ON claims(submission_date);
CREATE INDEX idx_claims_created_by      ON claims(created_by);

-- Denials
CREATE INDEX idx_denials_claim_id       ON denials(claim_id);
CREATE INDEX idx_denials_category       ON denials(denial_category);
CREATE INDEX idx_denials_resolution     ON denials(resolution_status);
CREATE INDEX idx_denials_date           ON denials(denial_date);

-- Follow-up tasks
CREATE INDEX idx_tasks_claim_id         ON follow_up_tasks(claim_id);
CREATE INDEX idx_tasks_assigned_user    ON follow_up_tasks(assigned_user_id);
CREATE INDEX idx_tasks_status_due       ON follow_up_tasks(task_status, due_date);

-- Claim status history
CREATE INDEX idx_history_claim_id       ON claim_status_history(claim_id);
CREATE INDEX idx_history_changed_at     ON claim_status_history(changed_at);

-- Payments
CREATE INDEX idx_payments_claim_id      ON payments(claim_id);
CREATE INDEX idx_payments_status        ON payments(payment_status);
CREATE INDEX idx_payments_date          ON payments(payment_date);

-- Audit logs
CREATE INDEX idx_audit_entity           ON audit_logs(entity_name, record_id);
CREATE INDEX idx_audit_user             ON audit_logs(user_id);
CREATE INDEX idx_audit_created_at       ON audit_logs(created_at);