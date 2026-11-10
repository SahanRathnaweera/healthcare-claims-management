-- =====================================================
-- 001_status_transition_validation.sql
-- Migration: status transition validation, mandatory
-- responsible user, and clear error messages.
-- Group AB | Member 10 - Database Lead
-- Safe to re-run. Does NOT delete existing data.
-- =====================================================

-- -----------------------------------------------------
-- 1. Allowed status transitions (rules stored as data)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS allowed_status_transitions (
    from_status  VARCHAR(20) NOT NULL,
    to_status    VARCHAR(20) NOT NULL,
    PRIMARY KEY (from_status, to_status),
    CONSTRAINT chk_ast_from CHECK (from_status IN
        ('SUBMITTED','PENDING','PAID','PARTIALLY_PAID','DENIED','APPEALED')),
    CONSTRAINT chk_ast_to CHECK (to_status IN
        ('SUBMITTED','PENDING','PAID','PARTIALLY_PAID','DENIED','APPEALED'))
);

INSERT INTO allowed_status_transitions (from_status, to_status) VALUES
('SUBMITTED',      'PENDING'),
('SUBMITTED',      'DENIED'),
('SUBMITTED',      'PAID'),
('SUBMITTED',      'PARTIALLY_PAID'),
('PENDING',        'PAID'),
('PENDING',        'PARTIALLY_PAID'),
('PENDING',        'DENIED'),
('PARTIALLY_PAID', 'PAID'),
('PARTIALLY_PAID', 'DENIED'),
('PARTIALLY_PAID', 'APPEALED'),
('DENIED',         'APPEALED'),
('APPEALED',       'PAID'),
('APPEALED',       'PARTIALLY_PAID'),
('APPEALED',       'DENIED')
ON CONFLICT DO NOTHING;
-- PAID has no outgoing rows, so it is a terminal status.

-- -----------------------------------------------------
-- 2. Make changed_by mandatory (without deleting data)
--    Existing NULL rows are back-filled with the claim
--    creator, or the first System Administrator.
-- -----------------------------------------------------
UPDATE claim_status_history h
SET changed_by = COALESCE(
        (SELECT c.created_by FROM claims c WHERE c.claim_id = h.claim_id),
        (SELECT u.user_id FROM users u
           JOIN roles r ON r.role_id = u.role_id
          WHERE r.role_name = 'System Administrator'
          ORDER BY u.user_id LIMIT 1)
    )
WHERE h.changed_by IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM claim_status_history WHERE changed_by IS NULL) THEN
        RAISE EXCEPTION 'Migration stopped: some claim_status_history rows still have NULL changed_by. Fix them manually first.';
    END IF;
END $$;

ALTER TABLE claim_status_history
    ALTER COLUMN changed_by SET NOT NULL;

-- -----------------------------------------------------
-- 3. New update_claim_status() with validation
-- -----------------------------------------------------
CREATE OR REPLACE PROCEDURE update_claim_status(
    p_claim_id   INT,
    p_new_status VARCHAR,
    p_user_id    INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_old_status VARCHAR(20);
    v_allowed    TEXT;
BEGIN
    -- (a) responsible user is required
    IF p_user_id IS NULL THEN
        RAISE EXCEPTION 'Responsible user ID is required'
            USING ERRCODE = 'P0002';
    END IF;

    -- (b) user must exist and be active
    IF NOT EXISTS (SELECT 1 FROM users
                    WHERE user_id = p_user_id AND is_active = TRUE) THEN
        RAISE EXCEPTION 'User % not found or inactive', p_user_id
            USING ERRCODE = 'P0003';
    END IF;

    -- (c) new status must be a valid status value
    IF p_new_status IS NULL OR p_new_status NOT IN
        ('SUBMITTED','PENDING','PAID','PARTIALLY_PAID','DENIED','APPEALED') THEN
        RAISE EXCEPTION 'Invalid status value: %', COALESCE(p_new_status, 'NULL')
            USING ERRCODE = 'P0004';
    END IF;

    -- (d) claim must exist (row locked for this transaction)
    SELECT current_status INTO v_old_status
    FROM claims
    WHERE claim_id = p_claim_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Claim % not found', p_claim_id
            USING ERRCODE = 'P0005';
    END IF;

    -- (e) same status is not a change
    IF v_old_status = p_new_status THEN
        RAISE EXCEPTION 'Claim % is already in status %', p_claim_id, p_new_status
            USING ERRCODE = 'P0006';
    END IF;

    -- (f) transition must be allowed
    IF NOT EXISTS (SELECT 1 FROM allowed_status_transitions
                    WHERE from_status = v_old_status AND to_status = p_new_status) THEN
        SELECT string_agg(to_status, ', ' ORDER BY to_status) INTO v_allowed
        FROM allowed_status_transitions
        WHERE from_status = v_old_status;

        RAISE EXCEPTION 'Invalid status transition: % -> %. Allowed from %: %',
            v_old_status, p_new_status, v_old_status,
            COALESCE(v_allowed, 'none (terminal status)')
            USING ERRCODE = 'P0007';
    END IF;

    -- (g) apply change + history in the same transaction
    UPDATE claims
       SET current_status = p_new_status,
           updated_at     = NOW()
     WHERE claim_id = p_claim_id;

    INSERT INTO claim_status_history (claim_id, previous_status, new_status, changed_by)
    VALUES (p_claim_id, v_old_status, p_new_status, p_user_id);
END;
$$;

-- -----------------------------------------------------
-- 4. Helper: list allowed next statuses (for frontend dropdown)
-- -----------------------------------------------------
CREATE OR REPLACE FUNCTION get_allowed_next_statuses(p_claim_id INT)
RETURNS TABLE (next_status VARCHAR)
LANGUAGE sql
STABLE
AS $$
    SELECT t.to_status
      FROM claims c
      JOIN allowed_status_transitions t ON t.from_status = c.current_status
     WHERE c.claim_id = p_claim_id
     ORDER BY t.to_status;
$$;