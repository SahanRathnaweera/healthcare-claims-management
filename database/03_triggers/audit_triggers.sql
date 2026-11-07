-- =====================================================
-- audit_triggers.sql
-- Triggers for audit logging

-- =====================================================

-- Generic audit function
CREATE OR REPLACE FUNCTION fn_audit_log()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_record_id INT;
    v_details   TEXT;
    v_user_id   INT;
BEGIN
    
    BEGIN
        v_user_id := NULLIF(current_setting('app.current_user_id', true), '')::INT;
    EXCEPTION WHEN OTHERS THEN
        v_user_id := NULL;
    END;

    IF TG_OP = 'INSERT' THEN
        v_record_id := (to_jsonb(NEW) ->> TG_ARGV[0])::INT;
        v_details   := 'Inserted: ' || to_jsonb(NEW)::TEXT;
    ELSIF TG_OP = 'UPDATE' THEN
        v_record_id := (to_jsonb(NEW) ->> TG_ARGV[0])::INT;
        v_details   := 'Updated from: ' || to_jsonb(OLD)::TEXT ||
                       ' to: ' || to_jsonb(NEW)::TEXT;
    ELSIF TG_OP = 'DELETE' THEN
        v_record_id := (to_jsonb(OLD) ->> TG_ARGV[0])::INT;
        v_details   := 'Deleted: ' || to_jsonb(OLD)::TEXT;
    END IF;

    INSERT INTO audit_logs (user_id, entity_name, record_id, action, details)
    VALUES (v_user_id, TG_TABLE_NAME, v_record_id, TG_OP, v_details);

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$;

-- Claims
DROP TRIGGER IF EXISTS trg_audit_claims ON claims;
CREATE TRIGGER trg_audit_claims
AFTER INSERT OR UPDATE OR DELETE ON claims
FOR EACH ROW EXECUTE FUNCTION fn_audit_log('claim_id');

-- Denials
DROP TRIGGER IF EXISTS trg_audit_denials ON denials;
CREATE TRIGGER trg_audit_denials
AFTER INSERT OR UPDATE OR DELETE ON denials
FOR EACH ROW EXECUTE FUNCTION fn_audit_log('denial_id');

-- Follow-up tasks
DROP TRIGGER IF EXISTS trg_audit_tasks ON follow_up_tasks;
CREATE TRIGGER trg_audit_tasks
AFTER INSERT OR UPDATE OR DELETE ON follow_up_tasks
FOR EACH ROW EXECUTE FUNCTION fn_audit_log('task_id');

-- Payments
DROP TRIGGER IF EXISTS trg_audit_payments ON payments;
CREATE TRIGGER trg_audit_payments
AFTER INSERT OR UPDATE OR DELETE ON payments
FOR EACH ROW EXECUTE FUNCTION fn_audit_log('payment_id');