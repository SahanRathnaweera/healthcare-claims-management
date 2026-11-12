# Authentication implementation

## Completed

- Added schema-matching role, user, and audit entities and repositories.
- Added BCrypt registration, inactive-by-default approval flow, JWT login, stateless logout blacklist, and `/me`.
- Added role authorities, method security, JSON security errors, validation, duplicate handling, and safe audit logging.
- Added [auth-db-changes.sql](./auth-db-changes.sql) for Member 10 review. It is not part of the existing database scripts.
- Added centralized RBAC URL rules in `security/AccessPolicy` and `SecurityConfig`.
- Roles are mapped from the four database `role_name` values to `ROLE_*` authorities in one place.
- Security failures return timestamped JSON with HTTP 401/403; denied requests are logged without tokens or passwords.
- `CurrentUserProvider` supplies the authenticated user id/authority for `created_by`, `changed_by`, and `recorded_by`.
- `UserManagementAuthorization` is the reusable service guard for administrator-only role changes, known roles,
  and preventing self-role changes or self-deactivation; user-management controllers must call it.

## API contract

| Endpoint | Method | Request | Success |
|---|---|---|---|
| `/api/auth/login` | POST | `email`, `password`, optional `rememberMe` | 200: token, expiry, user; 401/403 |
| `/api/auth/register` | POST | `fullName`, `email`, `password`, `confirmPassword` | 201: pending account; 400/409 |
| `/api/auth/logout` | POST | Bearer token | 200: message; 401 |
| `/api/auth/me` | GET | Bearer token | 200: profile; 401 |

## RBAC verification table

| Endpoint | Method | Role | Expected |
|---|---|---|---|
| `/api/users/**`, `/api/roles/**` | Any | System Administrator | 200 |
| `/api/claims/**` | GET | Any authenticated role | 200 |
| `/api/claims/**` | POST/PUT/PATCH | System Administrator, Claims Processing Officer | 200 |
| `/api/claims/{id}/status` | PUT/PATCH | System Administrator, Claims Processing Officer, Denial Management Officer | 200 |
| `/api/denials/**` | GET | Any authenticated role | 200 |
| `/api/denials/**` | POST/PUT/PATCH | System Administrator, Denial Management Officer | 200 |
| `/api/follow-up-tasks/**` | GET | Any authenticated role | 200 |
| `/api/follow-up-tasks/**` | POST | System Administrator, Denial Management Officer | 200 |
| `/api/follow-up-tasks/**` | PUT/PATCH | System Administrator, Claims Processing Officer, Denial Management Officer | 200 |
| `/api/payments/**` | GET | Any authenticated role | 200 |
| `/api/payments/**` | POST/PUT | System Administrator, Claims Processing Officer | 200 |
| `/api/audit-logs/**`, `/api/reports/**` | GET | System Administrator, Manager/Analyst | 200 |
| `/api/dashboard/**` | GET | Any authenticated role | 200 |
| `/api/chatbot/**` | Any | Any authenticated role | 200 |
| Any protected endpoint | Any | Missing/invalid/expired/blacklisted token | 401 |
| Any protected endpoint | Any | Authenticated role outside the rule | 403 |

## Local run

From `backend`, set `DB_USERNAME`, `DB_PASSWORD`, and a `JWT_SECRET` of at least 32 bytes, then run `./mvnw spring-boot:run`. The API listens on port 8081. Apply the review SQL after the base schema and seed data when login audit actions and development logins are required.

## Evidence and limitations

`mvnw -DskipTests compile` passes. The context test requires a reachable PostgreSQL database and valid environment variables. The blacklist is intentionally in memory and is lost on restart; there is no forgot-password flow, and new accounts remain inactive until administrator approval.
