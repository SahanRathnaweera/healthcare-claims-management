# User and Role Database Requirements

This document is for Member 10 (PostgreSQL Database Lead). It describes the API's database requirements for `users` and `roles`; it does not authorize application or schema changes in this repository.

## 1. Column requirements

### REQUIRED: `roles`

| Column | PostgreSQL type | Length | Nullability | API/constraint use |
|---|---|---:|---|---|
| `role_id` | `INT` | - | NOT NULL, primary key | Positive integer identifier |
| `role_name` | `VARCHAR(50)` | 50 characters | NOT NULL, UNIQUE | Exact seed/security name |

### REQUIRED: `users`

| Column | PostgreSQL type | Length | Nullability | API/constraint use |
|---|---|---:|---|---|
| `user_id` | `INT` | - | NOT NULL, primary key | Positive integer identifier |
| `full_name` | `VARCHAR(100)` | 100 characters | NOT NULL | Trimmed API input |
| `email` | `VARCHAR(100)` | 100 characters | NOT NULL, UNIQUE | Trimmed/lowercased API input |
| `password_hash` | `VARCHAR(255)` | 255 characters | NOT NULL | BCrypt hash only; never returned or logged |
| `role_id` | `INT` | - | NOT NULL, foreign key | Must reference `roles(role_id)` |
| `is_active` | `BOOLEAN` | - | NOT NULL, DEFAULT TRUE | Login/status eligibility |
| `created_at` | `TIMESTAMP` | - | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

## 2. Required constraints

Marking indicates the API requirement.

- **REQUIRED** - `users.email` has a UNIQUE constraint.
- **REQUIRED** - Add a unique, case-insensitive index: `CREATE UNIQUE INDEX ... ON users (LOWER(email));`.
- **REQUIRED** - `users.role_id` is NOT NULL.
- **REQUIRED** - `users.role_id` references `roles(role_id)` with a foreign key.
- **REQUIRED** - The role foreign key must have no cascade delete. Reject/handle role deletion so the API cannot orphan or silently delete users.
- **REQUIRED** - `users.full_name` is NOT NULL.
- **REQUIRED** - `users.email` is NOT NULL.
- **REQUIRED** - `users.password_hash` is NOT NULL.
- **REQUIRED** - `users.is_active` is NOT NULL and defaults to `TRUE`.
- **REQUIRED** - `users.created_at` is NOT NULL and defaults to `CURRENT_TIMESTAMP`.
- **REQUIRED** - `roles.role_name` is NOT NULL and UNIQUE.

## 3. API/database size alignment

- **REQUIRED** - `full_name` and API `@Size(max=100)` both allow 100 characters.
- **REQUIRED** - `email` and API `@Size(max=100)` both allow 100 characters.
- **REQUIRED** - `password_hash` is at least `VARCHAR(255)` so BCrypt output can fit.
- **REQUIRED** - `role_name` and API role names fit `VARCHAR(50)`.
- **REQUIRED** - BCrypt input passwords are API-validated to 72 characters because 72 bytes is the BCrypt limit. Password length is not the same as the `password_hash` column length.
- **RECOMMENDED** - Store emails in lowercase and trim whitespace before persistence.

## 4. Required seed data

- **REQUIRED** - Exactly four roles must exist, with these exact names matching [RoleName.java](../backend/src/main/java/com/healthcare/claimsmanagement/common/RoleName.java):
  1. `System Administrator`
  2. `Claims Processing Officer`
  3. `Denial Management Officer`
  4. `Manager/Analyst`
- **REQUIRED** - Create at least one active System Administrator.
- **REQUIRED** - Store a real BCrypt-generated password hash in `password_hash`; dummy or plaintext values are invalid.
- **RECOMMENDED** - Seed only known synthetic accounts and do not put credentials for real users in SQL.

## 5. Open questions

- **RECOMMENDED** - Will an audit trigger cover changes to `users`? Existing Java audit behavior currently covers registration and authentication events, while [audit_triggers.sql](../database/03_triggers/audit_triggers.sql) has triggers only for claims, denials, follow-up tasks, and payments. If a users trigger is added, Member 8 and Java should agree on ownership to avoid duplicate audit rows.
- **RECOMMENDED** - Which indexes should support user search/filter queries? Existing repository search uses full-name/email text matching, role, and active-status filters. Confirm the expected workload and add indexes only after plan review.

## 6. Verification SQL

Run the following in PostgreSQL and resolve unexpected rows or false checks.

```sql
-- Role and user column lengths and nullability
SELECT table_name, column_name, data_type, character_maximum_length, is_nullable,
       column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('users', 'roles')
ORDER BY table_name, ordinal_position;

-- Unique and primary key constraints
SELECT tc.table_name, tc.constraint_name, tc.constraint_type
FROM information_schema.table_constraints tc
WHERE tc.table_schema = 'public'
  AND tc.table_name IN ('users', 'roles')
ORDER BY tc.table_name, tc.constraint_type, tc.constraint_name;

-- Case-insensitive unique email index
SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename = 'users'
  AND indexdef ILIKE '%lower((email)::text)%';

-- Role foreign key and delete behavior
SELECT tc.constraint_name, rc.delete_rule,
       kcu.column_name AS foreign_column,
       ccu.table_name AS referenced_table,
       ccu.column_name AS referenced_column
FROM information_schema.table_constraints tc
JOIN information_schema.referential_constraints rc
  ON rc.constraint_name = tc.constraint_name
 AND rc.constraint_schema = tc.constraint_schema
JOIN information_schema.key_column_usage kcu
  ON kcu.constraint_name = tc.constraint_name
 AND kcu.constraint_schema = tc.constraint_schema
JOIN information_schema.constraint_column_usage ccu
  ON ccu.constraint_name = tc.constraint_name
 AND ccu.constraint_schema = tc.constraint_schema
WHERE tc.table_schema = 'public'
  AND tc.table_name = 'users'
  AND tc.constraint_type = 'FOREIGN KEY';

-- Exactly four required role names
SELECT role_name, COUNT(*) AS role_count
FROM roles
WHERE role_name IN (
    'System Administrator',
    'Claims Processing Officer',
    'Denial Management Officer',
    'Manager/Analyst'
)
GROUP BY role_name
ORDER BY role_name;

SELECT COUNT(DISTINCT role_name) AS matched_role_count,
       COUNT(*) AS role_row_count
FROM roles;

-- At least one active System Administrator
SELECT COUNT(*) AS active_system_administrators
FROM users u
JOIN roles r ON r.role_id = u.role_id
WHERE r.role_name = 'System Administrator'
  AND u.is_active = TRUE;

-- Search/filter indexes
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename = 'users'
ORDER BY indexname;

-- Password-hash and active/default checks
SELECT
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users'
          AND column_name = 'password_hash'
          AND data_type = 'character varying'
          AND character_maximum_length = 255
          AND is_nullable = 'NO'
    ) AS password_hash_255_not_null,
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users'
          AND column_name = 'is_active'
          AND data_type = 'boolean'
          AND is_nullable = 'NO'
          AND column_default ILIKE '%true%'
    ) AS is_active_not_null_default_true,
    EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'users'
          AND column_name = 'created_at'
          AND data_type = 'timestamp without time zone'
          AND is_nullable = 'NO'
          AND column_default IS NOT NULL
    ) AS created_at_not_null_defaulted;
```
