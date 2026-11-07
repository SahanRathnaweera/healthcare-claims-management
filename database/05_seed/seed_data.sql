-- =====================================================
-- seed_data.sql
-- Synthetic sample data (NO real patient information)
-- =====================================================

-- 1. ROLES
INSERT INTO roles (role_name) VALUES
('System Administrator'),
('Claims Processing Officer'),
('Denial Management Officer'),
('Manager/Analyst');

-- 2. USERS 
INSERT INTO users (full_name, email, password_hash, role_id) VALUES
('Admin User',        'admin@innobot.test',    '$2a$10$dummyhashadmin000000000000000000000000000000000000000', 1),
('Nimal Perera',      'nimal@innobot.test',    '$2a$10$dummyhashnimal000000000000000000000000000000000000000', 2),
('Kamala Silva',      'kamala@innobot.test',   '$2a$10$dummyhashkamala00000000000000000000000000000000000000', 2),
('Sunil Fernando',    'sunil@innobot.test',    '$2a$10$dummyhashsunil000000000000000000000000000000000000000', 3),
('Dilani Jayasinghe', 'dilani@innobot.test',   '$2a$10$dummyhashdilani00000000000000000000000000000000000000', 3),
('Ruwan Manager',     'ruwan@innobot.test',    '$2a$10$dummyhashruwan000000000000000000000000000000000000000', 4);

-- 3. CLAIMS (12 claims, sample statuses)
INSERT INTO claims (claim_reference, patient_ref, claim_amount, submission_date, current_status, created_by) VALUES
('CLM-2026-0001', 'PAT-1001', 25000.00, '2026-08-05', 'PAID',           2),
('CLM-2026-0002', 'PAT-1002', 48000.00, '2026-08-12', 'PARTIALLY_PAID', 2),
('CLM-2026-0003', 'PAT-1003', 15500.00, '2026-08-20', 'DENIED',         3),
('CLM-2026-0004', 'PAT-1004', 72000.00, '2026-09-02', 'PENDING',        2),
('CLM-2026-0005', 'PAT-1005', 33000.00, '2026-09-10', 'DENIED',         3),
('CLM-2026-0006', 'PAT-1006', 12000.00, '2026-09-15', 'SUBMITTED',      2),
('CLM-2026-0007', 'PAT-1007', 56000.00, '2026-09-22', 'APPEALED',       3),
('CLM-2026-0008', 'PAT-1008', 9800.00,  '2026-10-01', 'PAID',           2),
('CLM-2026-0009', 'PAT-1009', 41000.00, '2026-10-08', 'PENDING',        3),
('CLM-2026-0010', 'PAT-1010', 27500.00, '2026-10-15', 'DENIED',         2),
('CLM-2026-0011', 'PAT-1011', 63000.00, '2026-10-22', 'PARTIALLY_PAID', 3),
('CLM-2026-0012', 'PAT-1012', 18000.00, '2026-11-01', 'SUBMITTED',      2);

-- 4. DENIALS
INSERT INTO denials (claim_id, denial_category, denial_reason, denial_date, resolution_status, recorded_by) VALUES
(3,  'MISSING_INFO',   'Required diagnosis code missing',          '2026-08-25', 'IN_PROGRESS', 4),
(5,  'ELIGIBILITY',    'Patient coverage inactive on service date', '2026-09-18', 'OPEN',        4),
(7,  'AUTHORIZATION',  'Prior authorization not obtained',          '2026-09-28', 'APPEALED',    5),
(10, 'DUPLICATE_CLAIM','Duplicate of previously submitted claim',   '2026-10-20', 'OPEN',        5);

-- 5. FOLLOW-UP TASKS
INSERT INTO follow_up_tasks (claim_id, denial_id, assigned_user_id, title, description, priority, due_date, task_status) VALUES
(3,  1, 4, 'Add missing diagnosis code',   'Contact provider for diagnosis code',    'HIGH',   CURRENT_DATE + 3,  'IN_PROGRESS'),
(5,  2, 4, 'Verify patient eligibility',   'Check coverage dates with payer',        'URGENT', CURRENT_DATE + 1,  'OPEN'),
(7,  3, 5, 'Submit appeal documents',      'Prepare and send appeal package',        'HIGH',   CURRENT_DATE + 7,  'IN_PROGRESS'),
(10, 4, 5, 'Confirm duplicate claim',      'Compare with original claim CLM-2026-0002','MEDIUM', CURRENT_DATE + 5,  'OPEN'),
(4,  NULL, 2, 'Follow up pending claim',   'Check payer status after 30 days',       'LOW',    CURRENT_DATE + 14, 'OPEN');

-- 6. PAYMENTS
INSERT INTO payments (claim_id, amount_paid, payment_date, payment_method, payment_status, recorded_by) VALUES
(1,  25000.00, '2026-08-20', 'BANK_TRANSFER', 'PAID',           2),
(2,  20000.00, '2026-08-28', 'BANK_TRANSFER', 'PARTIALLY_PAID', 2),
(8,  9800.00,  '2026-10-10', 'CHEQUE',        'PAID',           2),
(11, 30000.00, '2026-11-03', 'BANK_TRANSFER', 'PARTIALLY_PAID', 3);

-- 7. CLAIM STATUS HISTORY
INSERT INTO claim_status_history (claim_id, previous_status, new_status, changed_by, changed_at) VALUES
(1, NULL,        'SUBMITTED', 2, '2026-08-05 09:00'),
(1, 'SUBMITTED', 'PENDING',   2, '2026-08-08 10:30'),
(1, 'PENDING',   'PAID',      2, '2026-08-20 14:00'),
(3, NULL,        'SUBMITTED', 3, '2026-08-20 09:15'),
(3, 'SUBMITTED', 'PENDING',   3, '2026-08-22 11:00'),
(3, 'PENDING',   'DENIED',    4, '2026-08-25 16:45'),
(7, NULL,        'SUBMITTED', 3, '2026-09-22 09:00'),
(7, 'SUBMITTED', 'DENIED',    5, '2026-09-28 13:20'),
(7, 'DENIED',    'APPEALED',  5, '2026-10-02 10:10');