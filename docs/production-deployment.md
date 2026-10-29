# Production Deployment — Trust Agro ERP

## Architecture (modular monolith)

```
Browser / SPA (React)
        |
        | HTTPS
        v
  Frontend (nginx static)     optional: same-origin /api proxy
        |
        | REACT_APP_API_URL → backend
        v
  Backend (Spring Boot 3, port 8081)
        |  JWT auth, department RBAC, Actuator /actuator/health
        v
  PostgreSQL 15
```

Single deployable backend JAR + static frontend. No microservices, Redis, or Kafka in this release.

---

## Profiles

| Profile | Database | DDL | Demo users | Notes |
|---------|----------|-----|------------|-------|
| `local` (default) | H2 file `./data/` | `update` | **yes** | Local DX only |
| `prod` | PostgreSQL via env | `validate` | **no** | Fail-fast config validator |
| `test` | H2 in-memory | `create-drop` | no | Maven Surefire |

Activate production with:

```bash
export SPRING_PROFILES_ACTIVE=prod
```

---

## Required environment variables (prod)

| Variable | Purpose |
|----------|---------|
| `APP_JWT_SECRET` | JWT HMAC secret, **≥ 32 characters** |
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://host:5432/trust_agro_db` |
| `DB_USERNAME` | DB user |
| `DB_PASSWORD` | DB password (must be non-blank) |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated explicit origins (not sole `*`) |

Optional: `SERVER_PORT`, `UPLOAD_DIR`, `REACT_APP_API_URL` (frontend build-time).

Copy `.env.example` → `.env` and fill values. **Never commit real secrets.**

`ProductionConfigValidator` (`@Profile("prod")`) aborts startup if JWT/password/CORS rules fail. Error messages do not echo secret values.

---

## Schema strategy

- **Flyway:** Spring Boot runs Flyway migrations automatically on startup from `classpath:db/migration/`.
  - Baseline migration `V1__initial_schema.sql` establishes all 129 tables, primary keys, identity sequences, indexes, and constraints.
  - `spring.flyway.baseline-on-migrate=true` and `spring.flyway.baseline-version=1` allow existing databases to adopt Flyway without reapplying the baseline script.
- **Hibernate validation:** In `prod`, `spring.jpa.hibernate.ddl-auto=validate` ensures the application's JPA entity mappings strictly match the Flyway-managed schema.
- **local:** `spring.jpa.hibernate.ddl-auto=update` with Flyway disabled for rapid local H2 iteration.

### PostgreSQL bootstrap (staging / empty database)

Because Flyway automatically applies `V1__initial_schema.sql` on backend startup, starting a clean staging or production stack requires no manual DDL commands:

```bash
# 1. Prepare .env with secrets and optional INITIAL_ADMIN_EMAIL / INITIAL_ADMIN_PASSWORD
cp .env.example .env

# 2. Start the full stack
docker compose up -d
```

The backend container will:
1. Wait for PostgreSQL to be healthy.
2. Execute Flyway migrations (`V1__initial_schema.sql`).
3. Validate entity mappings against the database schema (`ddl-auto=validate`).
4. Seed canonical role permissions via `RoleSeeder`.
5. Seed number sequences via `ProcurementDataInitializer` and `HrDataInitializer`.
6. Provision the initial administrator account via `InitialAdminProvisioner` (if `INITIAL_ADMIN_EMAIL` and `INITIAL_ADMIN_PASSWORD` are configured and no admin exists).

---

## Admin initialization

Demo credentials are **local profile only** (`app.seed.demo-users=true`).

For staging and production:
1. Supply `INITIAL_ADMIN_EMAIL` and `INITIAL_ADMIN_PASSWORD` in `.env`.
2. On initial startup with an empty database, `InitialAdminProvisioner` creates the administrator account with `Role.ADMIN` and a BCrypt-hashed password.
3. If an administrator account already exists, `InitialAdminProvisioner` cleanly skips creation.
4. If `INITIAL_ADMIN_EMAIL` / `INITIAL_ADMIN_PASSWORD` are left blank, the application starts with zero users and logs a clear setup notice, allowing administrators to be inserted via secure out-of-band management.

---

## Health checks

- Actuator: `GET /actuator/health` (and Kubernetes probes via `management.endpoint.health.probes.enabled=true`)
- Info: `GET /actuator/info`
- These endpoints are permitted without JWT.
- Local `start-dev.sh` waits on `/actuator/health`.

Example:

```bash
curl -sf http://localhost:8081/actuator/health
```

---

## Correlation ID

Every request gets `X-Request-Id` (incoming or generated UUID). It is stored in MDC as `requestId` and returned on API responses (`ApiResponse.requestId`) and the response header for support/troubleshooting.

---

## Docker Compose

From the repo root (after filling `.env`):

```bash
cp .env.example .env
# edit .env — set APP_JWT_SECRET, DB_PASSWORD, CORS, REACT_APP_API_URL
# If the host already uses 5432, set POSTGRES_PORT=5434 (or another free port)

# Build artifacts on the host (images copy these; they do not run Maven/npm inside Docker)
cd backend && mvn -B test package && cd ..
cd frontend && npm ci && CI=true npm test -- --watchAll=false && CI=true npm run build && cd ..

docker compose build
docker compose up -d
```

Services: `postgres:15`, `backend` (`SPRING_PROFILES_ACTIVE=prod`, port `${BACKEND_PORT:-8081}`), `frontend` (nginx on `${FRONTEND_PORT:-3000}`).

Backend image: Temurin JRE 17, non-root user, copies `backend/target/trust-agro-backend-1.0.0.jar`, healthcheck on Actuator.

Frontend image: nginx copies `frontend/build/` (built on the host with `REACT_APP_API_URL`). Rebuild the frontend image after changing that URL.

Optional: `COMPOSE_PROJECT_NAME=trustagro_staging` to isolate volumes from other Compose projects on the same host.

---

## Backup

### Dump

```bash
pg_dump -h localhost -U trustagro -d trust_agro_db -Fc -f trust_agro_$(date +%Y%m%d).dump
```

### Restore

Restore onto a **separate** database. Never restore over the live staging/production database as the first test.

```bash
# inside the postgres container, or with PGPASSWORD from your secret store
createdb -h localhost -U trustagro trust_agro_restore_test
pg_restore -h localhost -U trustagro -d trust_agro_restore_test --no-owner trust_agro_YYYYMMDD.dump
```

Overwriting the live database is a last-resort recovery step, not a validation procedure.

Store dumps encrypted offline. Test restores on a staging database regularly.

### Restore verification

1. `pg_isready` / connect with `psql`.
2. Spot-check row counts for critical tables (`users`, `farms`, finance ledgers).
3. Start backend with `prod` profile; confirm `/actuator/health` is `UP`.
4. Login as a known admin; open one read-only screen per department.
5. Confirm no demo users were created (`app.seed.demo-users=false`).

---

## Rollback

- **Application:** redeploy the previous Docker image / JAR. Keep the prior artifact tagged.
- **Schema:** not auto-rolled back. If a release required DDL, restore from `pg_dump` or apply a prepared reverse migration. Prefer expand/contract migrations once Flyway is adopted.
- **Config:** revert env vars carefully; rotating JWT immediately invalidates all sessions (see below).

---

## JWT rotation

1. Schedule a maintenance window (all sessions will be invalidated).
2. Generate a new secret ≥ 32 characters; set `APP_JWT_SECRET`.
3. Restart backend instances.
4. Users must log in again.
5. Never log or commit the secret. Do not put secrets in image layers — use runtime env / secret manager.

---

## Troubleshooting

| Symptom | Check |
|---------|--------|
| Startup fails: JWT / DB_PASSWORD / CORS | Env vars for `prod`; see validator message (no secret echo) |
| `Schema-validation` errors | Prod uses `validate`; apply missing DDL before deploy |
| CORS browser errors | `APP_CORS_ALLOWED_ORIGINS` must match the SPA origin exactly |
| 401 loops on frontend | Token expired or JWT secret rotated; clear storage and login |
| Health down | Postgres connectivity, Hikari pool, container logs |
| Conflict `409` | Unique/constraint violation — safe message only, no SQL leaked |
| Need request trace | Use `X-Request-Id` / `requestId` in API JSON |

Local logs that may contain tokens (`admin_token.txt`, `*_log.txt`) are gitignored — do not commit them. Historical files already in git history are not rewritten by this step.

---

## Related

- Root [README.md](../README.md) — local development
- `.env.example` — placeholder env vars
- `docker-compose.yml` — reference compose stack
