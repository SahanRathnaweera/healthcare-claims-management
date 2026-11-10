-- Review-only changes for authentication audit events and development users.
ALTER TABLE audit_logs DROP CONSTRAINT audit_logs_action_check;
ALTER TABLE audit_logs ADD CONSTRAINT audit_logs_action_check
    CHECK (action IN ('INSERT','UPDATE','DELETE','LOGIN','LOGOUT','LOGIN_FAILED'));

UPDATE users SET password_hash = '$2a$10$q3T6l3h3qcVc5vyZVFo8nOht/cxxQF2PrfgJ8rQHhSJctPRKy3NH2' WHERE email = 'admin@innobot.test';
UPDATE users SET password_hash = '$2a$10$v6FLa7RK3aSFPEAcU.k/auKGcc3BxvHg2Bm64RDI2GLSMyo0rjZsq' WHERE email = 'nimal@innobot.test';
UPDATE users SET password_hash = '$2a$10$k/sYzCCnwtu7a9ECvPIDHe.xcRA4mAqt1zqj7pvGIVQ8X5DIQZI6W' WHERE email = 'kamala@innobot.test';
UPDATE users SET password_hash = '$2a$10$WUr8FyW2YQsitUIUxHIT/u1hoUwmvlz0/84V0YTJM52G92K2o.toe' WHERE email = 'sunil@innobot.test';
UPDATE users SET password_hash = '$2a$10$mqznfTAuqoYLj2otLj.QAO2TxEZdRBed9T/KS6FuyaiGtcvZU7pyu' WHERE email = 'dilani@innobot.test';
UPDATE users SET password_hash = '$2a$10$AsM8ws.IjNkozmR7UzOVf.ZIV3PsexsHnTRXP1DBP2l6WejbNqLm.' WHERE email = 'ruwan@innobot.test';
