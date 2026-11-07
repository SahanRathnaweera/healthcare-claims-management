-- =====================================================
-- stored_procedures.sql
-- Stored procedures / functions
-- =====================================================


CREATE OR REPLACE PROCEDURE update_claim_status(
    p_claim_id   INT,
    p_new_status VARCHAR,
    p_user_id    INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_old_status VARCHAR(20);
BEGIN
    SELECT current_status INTO v_old_status
    FROM claims
    WHERE claim_id = p_claim_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Claim % not found', p_claim_id;
    END IF;

    IF v_old_status = p_new_status THEN
        RAISE EXCEPTION 'Claim % is already in status %', p_claim_id, p_new_status;
    END IF;

    UPDATE claims
    SET current_status = p_new_status,
        updated_at = NOW()
    WHERE claim_id = p_claim_id;

    INSERT INTO claim_status_history (claim_id, previous_status, new_status, changed_by)
    VALUES (p_claim_id, v_old_status, p_new_status, p_user_id);
END;
$$;


CREATE OR REPLACE PROCEDURE record_denial_with_task(
    p_claim_id     INT,
    p_category     VARCHAR,
    p_reason       TEXT,
    p_user_id      INT,
    p_assigned_to  INT,
    p_due_date     DATE
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_denial_id INT;
BEGIN
    INSERT INTO denials (claim_id, denial_category, denial_reason, recorded_by)
    VALUES (p_claim_id, p_category, p_reason, p_user_id)
    RETURNING denial_id INTO v_denial_id;

    INSERT INTO follow_up_tasks (claim_id, denial_id, assigned_user_id, title, priority, due_date)
    VALUES (p_claim_id, v_denial_id, p_assigned_to,
            'Follow up on denial #' || v_denial_id, 'HIGH', p_due_date);

    CALL update_claim_status(p_claim_id, 'DENIED', p_user_id);
END;
$$;


CREATE OR REPLACE FUNCTION get_outstanding_balance(p_claim_id INT)
RETURNS NUMERIC
LANGUAGE plpgsql
AS $$
DECLARE
    v_total NUMERIC;
    v_paid  NUMERIC;
BEGIN
    SELECT claim_amount INTO v_total FROM claims WHERE claim_id = p_claim_id;
    SELECT COALESCE(SUM(amount_paid), 0) INTO v_paid
    FROM payments WHERE claim_id = p_claim_id;
    RETURN v_total - v_paid;
END;
$$;