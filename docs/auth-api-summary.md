# Authentication implementation

## Completed

- Added schema-matching role, user, and audit entities and repositories.
- Added BCrypt registration, inactive-by-default approval flow, JWT login, stateless logout blacklist, and `/me`.
- Added role authorities, method security, JSON security errors, validation, duplicate handling, and safe audit logging.
- Added [auth-db-changes.sql](./auth-db-changes.sql) for Member 10 review. It is not part of the existing database scripts.

## API contract

| Endpoint | Method | Request | Success |
|---|---|---|---|
| `/api/auth/login` | POST | `email`, `password`, optional `rememberMe` | 200: token, expiry, user; 401/403 |
| `/api/auth/register` | POST | `fullName`, `email`, `password`, `confirmPassword` | 201: pending account; 400/409 |
| `/api/auth/logout` | POST | Bearer token | 200: message; 401 |
| `/api/auth/me` | GET | Bearer token | 200: profile; 401 |

## Local run

From `backend`, set `DB_USERNAME`, `DB_PASSWORD`, and a `JWT_SECRET` of at least 32 bytes, then run `./mvnw spring-boot:run`. The API listens on port 8081. Apply the review SQL after the base schema and seed data when login audit actions and development logins are required.

## Evidence and limitations

`mvnw -DskipTests compile` passes. The context test requires a reachable PostgreSQL database and valid environment variables. The blacklist is intentionally in memory and is lost on restart; there is no forgot-password flow, and new accounts remain inactive until administrator approval.
