-- =====================================================
-- reporting_views.sql
-- Views for dashboard and reports
-- =====================================================

-- 1. Claim count by status (dashboard KPI)
CREATE OR REPLACE VIEW v_claim_status_summary AS
SELECT current_status,
       COUNT(*)           AS claim_count,
       SUM(claim_amount)  AS total_amount
FROM claims
GROUP BY current_status;

-- 2. Denial category summary
CREATE OR REPLACE VIEW v_denial_category_summary AS
SELECT denial_category,
       COUNT(*) AS denial_count,
       COUNT(*) FILTER (WHERE resolution_status IN ('RESOLVED','CLOSED')) AS resolved_count,
       COUNT(*) FILTER (WHERE resolution_status IN ('OPEN','IN_PROGRESS','APPEALED')) AS unresolved_count
FROM denials
GROUP BY denial_category;

-- 3. Outstanding payments per claim
CREATE OR REPLACE VIEW v_outstanding_payments AS
SELECT c.claim_id,
       c.claim_reference,
       c.claim_amount,
       COALESCE(SUM(p.amount_paid), 0)                      AS total_paid,
       c.claim_amount - COALESCE(SUM(p.amount_paid), 0)     AS outstanding_balance,
       c.current_status
FROM claims c
LEFT JOIN payments p ON p.claim_id = c.claim_id
GROUP BY c.claim_id, c.claim_reference, c.claim_amount, c.current_status
HAVING c.claim_amount - COALESCE(SUM(p.amount_paid), 0) > 0;

-- 4. Overdue / open follow-up tasks
CREATE OR REPLACE VIEW v_overdue_tasks AS
SELECT t.task_id,
       t.title,
       c.claim_reference,
       u.full_name AS assigned_to,
       t.priority,
       t.due_date,
       t.task_status,
       (CURRENT_DATE - t.due_date) AS days_overdue
FROM follow_up_tasks t
JOIN claims c ON c.claim_id = t.claim_id
JOIN users  u ON u.user_id  = t.assigned_user_id
WHERE t.task_status IN ('OPEN','IN_PROGRESS')
  AND t.due_date < CURRENT_DATE;

-- 5. Monthly claim trend
CREATE OR REPLACE VIEW v_monthly_claim_trend AS
SELECT date_trunc('month', submission_date)::date AS month,
       COUNT(*)          AS claims_submitted,
       SUM(claim_amount) AS total_amount
FROM claims
GROUP BY date_trunc('month', submission_date)
ORDER BY month;