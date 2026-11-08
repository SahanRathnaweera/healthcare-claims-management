# Backup and Restore Guide

**Database:** `healthcare_claims_db`
**DBMS:** PostgreSQL 17
**Owner:** Member 10 (Database Lead), Group AB

This guide describes how to back up the prototype database and restore it after data loss or failure. Only synthetic sample data is stored in this database.

---

## 1. Backup using pgAdmin

1. Open pgAdmin and connect to the **PostgreSQL 17** server.
2. In Object Explorer, right-click `healthcare_claims_db`.
3. Click **Backup...**
4. Set the following options:
   - **Filename:** `healthcare_claims_backup.backup`
   - **Format:** `Custom`
   - **Encoding:** `UTF8`
5. Click **Backup**.
6. Wait for the "Backup completed" notification.

---

## 2. Restore using pgAdmin

1. Create an empty database: right-click **Databases**, then **Create > Database...** and name it `healthcare_claims_db_restore`.
2. Right-click `healthcare_claims_db_restore` and click **Restore...**
3. Set the following options:
   - **Format:** `Custom or tar`
   - **Filename:** select `healthcare_claims_backup.backup`
4. Click **Restore**.
5. Wait for the "Restore completed" notification.

---

## 3. Backup and restore using the command line (optional)

The PostgreSQL `bin` folder must be on the system PATH
(for example `C:\Program Files\PostgreSQL\17\bin`).

```bash
# Backup (custom format)
pg_dump -U postgres -d healthcare_claims_db -F c -f healthcare_claims_backup.dump

# Create an empty database for the restore
createdb -U postgres healthcare_claims_db_restore

# Restore
pg_restore -U postgres -d healthcare_claims_db_restore healthcare_claims_backup.dump
```

A plain SQL backup can also be created:

```bash
pg_dump -U postgres -d healthcare_claims_db -f healthcare_claims_backup.sql
psql -U postgres -d healthcare_claims_db_restore -f healthcare_claims_backup.sql
```

---

## 4. Recovery test (evidence for the report)

Run the test on the **restore database**, not on the original database.

1. Take a fresh backup of `healthcare_claims_db` (Section 1).
2. Restore it into `healthcare_claims_db_restore` (Section 2).
3. Record the row count in the restored database:
   ```sql
   SELECT COUNT(*) FROM payments;
   ```
4. Simulate data loss in the restored database:
   ```sql
   DELETE FROM payments;
   SELECT COUNT(*) FROM payments;   -- expect 0
   ```
5. Drop and recreate the restore database, then restore the backup again (Section 2).
6. Verify that the data has been recovered:
   ```sql
   SELECT COUNT(*) FROM payments;   -- expect the original count
   ```
7. Take screenshots of steps 3, 4 and 6 as test evidence.

---

## 5. Notes

- Do not push backup files (`.backup`, `.dump`, `.sql` dumps) to GitHub. Add them to `.gitignore`:
  ```
  *.backup
  *.dump
  ```
- Take a backup before running any destructive script such as `02_create_tables.sql`, which drops and recreates all tables.
- Use synthetic or authorized sample data only. Do not store real patient information in the prototype.