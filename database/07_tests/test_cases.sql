-- =====================================================
-- test_cases.sql
-- Database test evidence
-- =====================================================

-- TEST 1: Stored procedure - claim status update (should succeed)
CALL update_claim_status(6, 'PENDING', 2);
SELECT current_status FROM claims WHERE claim_id = 6;            -- expect PENDING
SELECT * FROM claim_status_history WHERE claim_id = 6;           -- expect new history row

-- TEST 2: Same status again (should FAIL with exception)
CALL update_claim_status(6, 'PENDING', 2);

-- TEST 3: Invalid claim id (should FAIL: Claim 999 not found)
CALL update_claim_status(999, 'PAID', 2);

-- TEST 4: Transaction - record denial + task together
CALL record_denial_with_task(9, 'MISSING_INFO', 'Missing referral note', 4, 4, CURRENT_DATE + 5);
SELECT * FROM denials WHERE claim_id = 9;
SELECT * FROM follow_up_tasks WHERE claim_id = 9;
SELECT current_status FROM claims WHERE claim_id = 9;            -- expect DENIED

-- TEST 5: Transaction rollback (invalid category -> everything rolls back)
CALL record_denial_with_task(12, 'INVALID_CAT', 'test', 4, 4, CURRENT_DATE + 5);
SELECT COUNT(*) FROM denials WHERE claim_id = 12;                -- expect 0

-- TEST 6: CHECK constraint - negative amount (should FAIL)
INSERT INTO claims (claim_reference, patient_ref, claim_amount)
VALUES ('CLM-TEST-001', 'PAT-9999', -500);

-- TEST 7: UNIQUE constraint - duplicate claim reference (should FAIL)
INSERT INTO claims (claim_reference, patient_ref, claim_amount)
VALUES ('CLM-2026-0001', 'PAT-9999', 1000);

-- TEST 8: FK constraint - invalid claim id in payments (should FAIL)
INSERT INTO payments (claim_id, amount_paid) VALUES (9999, 100);

-- TEST 9: Audit trigger - update a claim and check audit_logs
UPDATE claims SET claim_amount = 26000 WHERE claim_id = 1;
SELECT * FROM audit_logs WHERE entity_name = 'claims' AND record_id = 1
ORDER BY created_at DESC;                                        -- expect UPDATE row

-- TEST 10: Function - outstanding balance
SELECT get_outstanding_balance(2);                               -- expect 28000.00

-- TEST 11: Views
SELECT * FROM v_claim_status_summary;
SELECT * FROM v_denial_category_summary;
SELECT * FROM v_outstanding_payments;
SELECT * FROM v_overdue_tasks;
SELECT * FROM v_monthly_claim_trend;

-- TEST 12: Index usage (query plan)
EXPLAIN ANALYZE SELECT * FROM claims WHERE current_status = 'DENIED';