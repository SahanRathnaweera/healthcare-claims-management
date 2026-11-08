# Database Module

**Project:** Healthcare Claims Tracking and Denial Management System (Innobot Health, Group AB)
**Owner:** Member 10 (PostgreSQL Database Lead)
**DBMS:** PostgreSQL 17 (administered with pgAdmin 4)
**Database name:** `healthcare_claims_db`

> All data is synthetic. Do not store real patient information in this prototype.

---

## Folder structure

```
database/
├── 01_schema/
│   ├── 01_create_database.sql
│   ├── 02_create_tables.sql
│   ├── 03_constraints.sql
│   └── 04_indexes.sql
├── 02_functions/
│   └── stored_procedures.sql
├── 03_triggers/
│   └── audit_triggers.sql
├── 04_views/
│   └── reporting_views.sql
├── 05_seed/
│   └── seed_data.sql
├── 06_backup/
│   └── backup_restore_guide.md
├── 07_tests/
│   └── test_cases.sql
└── README.md
```

## How to run (in this order)

1. `01_schema/01_create_database.sql`
2. `01_schema/02_create_tables.sql`
3. `01_schema/03_constraints.sql`
4. `01_schema/04_indexes.sql`
5. `02_functions/stored_procedures.sql`
6. `03_triggers/audit_triggers.sql`
7. `04_views/reporting_views.sql`
8. `05_seed/seed_data.sql`

Run these in pgAdmin Query Tool while connected to `healthcare_claims_db`
(except step 1, which is run on the default `postgres` database).

> Warning: `02_create_tables.sql` drops and recreates all tables. Do not run it on a database that contains data you want to keep.

---

## Tables

| Table | Purpose | Key columns |
|---|---|---|
| `roles` | User roles | `role_id`, `role_name` |
| `users` | System users | `user_id`, `email`, `role_id` (FK), `is_active` |
| `claims` | Claim records | `claim_id`, `claim_reference` (UNIQUE), `claim_amount`, `current_status` |
| `denials` | Denial cases | `denial_id`, `claim_id` (FK), `denial_category`, `resolution_status` |
| `follow_up_tasks` | Follow-up work items | `task_id`, `claim_id` (FK), `denial_id` (FK), `assigned_user_id` (FK), `priority`, `due_date`, `task_status` |
| `claim_status_history` | Status change history | `history_id`, `claim_id` (FK), `previous_status`, `new_status`, `changed_by` (FK) |
| `payments` | Payment records | `payment_id`, `claim_id` (FK), `amount_paid`, `payment_status` |
| `audit_logs` | Audit trail | `audit_id`, `user_id` (FK), `entity_name`, `record_id`, `action` |

## Allowed values

| Column | Values |
|---|---|
| `claims.current_status` | `SUBMITTED`, `PENDING`, `PAID`, `PARTIALLY_PAID`, `DENIED`, `APPEALED` |
| `denials.denial_category` | `MISSING_INFO`, `INVALID_CODE`, `ELIGIBILITY`, `DUPLICATE_CLAIM`, `TIMELY_FILING`, `AUTHORIZATION`, `OTHER` |
| `denials.resolution_status` | `OPEN`, `IN_PROGRESS`, `APPEALED`, `RESOLVED`, `CLOSED` |
| `follow_up_tasks.priority` | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `follow_up_tasks.task_status` | `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `payments.payment_status` | `PAID`, `PARTIALLY_PAID`, `UNPAID`, `OVERDUE` |
| `audit_logs.action` | `INSERT`, `UPDATE`, `DELETE` |

---

## Stored procedures and functions (for backend developers)

| Name | Type | Usage |
|---|---|---|
| `update_claim_status(claim_id, new_status, user_id)` | Procedure | Updates a claim status and records the change in `claim_status_history` in one transaction |
| `record_denial_with_task(claim_id, category, reason, user_id, assigned_to, due_date)` | Procedure | Creates a denial, a follow-up task and sets the claim to `DENIED` in one transaction |
| `get_outstanding_balance(claim_id)` | Function | Returns claim amount minus total payments |

Example:

```sql
CALL update_claim_status(6, 'PENDING', 2);
SELECT get_outstanding_balance(2);
```

## Views (for dashboard and reports)

| View | Used by |
|---|---|
| `v_claim_status_summary` | Dashboard KPI cards |
| `v_denial_category_summary` | Denial analysis report |
| `v_outstanding_payments` | Outstanding balances report |
| `v_overdue_tasks` | Overdue follow-ups |
| `v_monthly_claim_trend` | Trend charts |

## Audit logging

Triggers on `claims`, `denials`, `follow_up_tasks` and `payments` write to `audit_logs`
on every INSERT, UPDATE and DELETE.

To record which user made the change, the backend should run this at the start of each transaction:

```sql
SET LOCAL app.current_user_id = '<user_id>';
```

If it is not set, `audit_logs.user_id` is stored as NULL.

---

## Seed data

`05_seed/seed_data.sql` inserts 4 roles, 6 users, 12 claims, 4 denials, 5 follow-up tasks, 4 payments and 9 status history rows.
The `password_hash` values are placeholders. The backend must replace them with real BCrypt hashes.

## Tests

`07_tests/test_cases.sql` contains 12 tests covering procedures, transaction rollback,
constraints, audit triggers, the function, views and index usage.
Run each test separately and keep the screenshots as evidence.

## Backup and restore

See `06_backup/backup_restore_guide.md`.

## Connection settings (local development)

| Setting | Value |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `healthcare_claims_db` |
| Username | `postgres` |
| Password | Do not commit. Each member uses their own local password. |