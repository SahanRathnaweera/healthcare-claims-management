-- =====================================================
-- 03_constraints.sql
-- Extra business-rule constraints

-- =====================================================

ALTER TABLE users
    ADD CONSTRAINT chk_users_email_format
    CHECK (email LIKE '%_@_%._%');

ALTER TABLE follow_up_tasks
    ADD CONSTRAINT chk_task_due_date
    CHECK (due_date >= created_at::date);

ALTER TABLE follow_up_tasks
    ADD CONSTRAINT chk_task_completed_at
    CHECK (
        (task_status = 'COMPLETED' AND completed_at IS NOT NULL)
        OR (task_status <> 'COMPLETED')
    );

ALTER TABLE claim_status_history
    ADD CONSTRAINT chk_status_changed
    CHECK (previous_status IS DISTINCT FROM new_status);

ALTER TABLE claim_status_history
    ADD CONSTRAINT chk_history_new_status
    CHECK (new_status IN
        ('SUBMITTED','PENDING','PAID','PARTIALLY_PAID','DENIED','APPEALED'));

ALTER TABLE payments
    ADD CONSTRAINT chk_payment_date_not_future
    CHECK (payment_date <= CURRENT_DATE);

ALTER TABLE denials
    ADD CONSTRAINT chk_denial_date_not_future
    CHECK (denial_date <= CURRENT_DATE);

ALTER TABLE denials
    ADD CONSTRAINT chk_denial_category
    CHECK (denial_category IN
        ('MISSING_INFO','INVALID_CODE','ELIGIBILITY','DUPLICATE_CLAIM',
         'TIMELY_FILING','AUTHORIZATION','OTHER'));