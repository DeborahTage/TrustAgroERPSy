# Production Deployment — Trust Agro ERP

## Architecture (modular monolith)

```
Browser / SPA (React)
        |
        | HTTPS
        v
  Frontend (nginx static + same-origin /api proxy)
        |
        | /api → backend (internal network)
        v
  Backend (Spring Boot 3, internal port 8081)
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
| `APP_JWT_SECRET` | Generated JWT HMAC secret, **≥ 32 characters** |
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://host:5432/trust_agro_db` |
| `DB_USERNAME` | Least-privilege application database role (must not be `postgres`) |
| `DB_PASSWORD` | Generated application-role password, **≥ 24 characters** |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated explicit origins (not sole `*`) |
| `DB_BOOTSTRAP_USERNAME` | PostgreSQL container initialization/superuser account; never used by the backend |
| `DB_BOOTSTRAP_PASSWORD` | Generated bootstrap password; never used by the backend |
| `INITIAL_ADMIN_EMAIL` | Optional one-time administrator email; required with its password only on an empty install |
| `INITIAL_ADMIN_PASSWORD` | One-time generated administrator password, **≥ 16 characters** |

Optional: `SERVER_PORT`, `UPLOAD_DIR`.

Copy `.env.example` → `.env`; generate separate values for the bootstrap password,
application database password, and JWT secret (for example with
`openssl rand -base64 48`). Never reuse the bootstrap credentials for the backend
and never commit `.env` or generated credentials. The backend refuses known
example placeholders, weak production passwords, and the PostgreSQL superuser.
If the database already contains an administrator, leave the one-time admin
variables blank. On a clean database, set them explicitly or provision the first
administrator through an approved out-of-band process; production startup fails
closed rather than starting with no administrator.

`ProductionConfigValidator` (`@Profile("prod")`) aborts startup if required
secret, runtime-role, database, seed, or CORS rules fail. Error messages do not
echo secret values.

---

## Schema strategy

- **Flyway:** Spring Boot runs Flyway migrations automatically on startup from `classpath:db/migration/`.
  - `V1__initial_schema.sql` establishes the baseline schema; subsequent migrations apply reviewed schema changes.
  - `spring.flyway.baseline-on-migrate=true` and `spring.flyway.baseline-version=1` allow existing databases to adopt Flyway without reapplying the baseline script.
- **Hibernate validation:** In `prod`, `spring.jpa.hibernate.ddl-auto=validate` ensures the application's JPA entity mappings strictly match the Flyway-managed schema.
- **local:** `spring.jpa.hibernate.ddl-auto=update` with Flyway disabled for rapid local H2 iteration.

### PostgreSQL bootstrap (staging / empty database)

Because Flyway automatically applies pending migrations on backend startup, starting a clean staging or production stack requires no manual application DDL commands:

```bash
# 1. Prepare .env with secrets and optional INITIAL_ADMIN_EMAIL / INITIAL_ADMIN_PASSWORD
cp .env.example .env

# 2. Start the full stack
docker compose up -d
```

The backend container will:
1. Wait for PostgreSQL to be healthy.
2. Execute Flyway migrations V1 through V3.
3. Validate entity mappings against the database schema (`ddl-auto=validate`).
4. Seed canonical role permissions via `RoleSeeder`.
5. Seed number sequences via `ProcurementDataInitializer` and `HrDataInitializer`.
6. Provision the initial administrator account via `InitialAdminProvisioner` when one-time credentials are configured and no admin exists; startup fails closed if a clean database has no administrator credentials.

---

## Admin initialization

Demo credentials are **local profile only** (`app.seed.demo-users=true`).

For staging and production:
1. Supply `INITIAL_ADMIN_EMAIL` and `INITIAL_ADMIN_PASSWORD` in `.env`.
2. On initial startup with an empty database, `InitialAdminProvisioner` creates the administrator account with `Role.ADMIN` and a BCrypt-hashed password.
3. If an administrator account already exists, `InitialAdminProvisioner` cleanly skips creation.
4. If one-time initial administrator credentials are left blank and no administrator exists, production startup fails. Provision through an approved out-of-band process or set generated one-time credentials.

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
# edit .env — provide generated bootstrap, application DB, and JWT credentials plus CORS

# Build artifacts on the host (images copy these; they do not run Maven/npm inside Docker)
cd backend && mvn -B test package && cd ..
cd frontend && npm ci && CI=true npm test -- --watchAll=false && CI=true npm run build && cd ..

docker compose build
docker compose up -d
```

Services: `postgres:15`, `backend` (`SPRING_PROFILES_ACTIVE=prod`, internal port 8081), and `frontend` (nginx on `${FRONTEND_PORT:-3000}`). PostgreSQL and backend ports are internal to the Compose network; the frontend proxies `/api/` to the backend on the same origin. The local CRA development server proxies API requests to `localhost:8081`.

On a new PostgreSQL volume, the initialization script creates the configured
application role with database-connect and public-schema usage/create privileges;
the application connects only as this role. Initialization scripts run only
when PostgreSQL creates an empty data directory. For an existing volume, have a
database administrator create the application login and assess ownership of
existing application tables and sequences. Granting CONNECT and USAGE/CREATE on
`public` alone is not sufficient: Flyway must be able to alter its existing
objects, so the runtime role must own those objects (or the database must be
migrated under an explicitly approved ownership strategy). Then set the runtime
`DB_USERNAME` and `DB_PASSWORD`. Do not change the stored PostgreSQL bootstrap
credentials to the application credentials.

Backend image: Temurin JRE 17, non-root user, copies `backend/target/trust-agro-backend-1.0.0.jar`, healthcheck on Actuator.

Frontend image: nginx copies `frontend/build/`. The build uses relative `/api/...`
requests, which nginx proxies to the backend; no API hostname is embedded in the
production bundle.

Optional: `COMPOSE_PROJECT_NAME=trustagro_staging` to isolate volumes from other Compose projects on the same host.

---

## Backup

### Dump

```bash
docker compose exec -T postgres sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' \
  > trust_agro_$(date +%Y%m%d).dump
```

### Restore

Restore onto a **separate** database. Never restore over the live staging/production database as the first test.

```bash
docker compose exec postgres sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" createdb -U "$POSTGRES_USER" trust_agro_restore_test'
docker compose exec -T postgres sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_restore -U "$POSTGRES_USER" -d trust_agro_restore_test --no-owner' \
  < trust_agro_YYYYMMDD.dump
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

## Rate limiting & horizontal scaling

Login rate limiting is implemented in-memory (`AuthService`) with an eviction-bounded window (5 failed attempts per 15-minute window per IP and per email account).

**Single-Instance Architecture Note:**
- The in-memory tracker is local to the JVM instance and resets on container restart.
- In a horizontally scaled deployment (multiple backend containers behind a load balancer), rate-limiting state is not synchronized across instances.
- **Pre-Scaling Recommendation:** Before scaling backend services beyond a single container, implement a shared rate-limiting store (such as Redis token buckets) or configure ingress rate limiting at the reverse-proxy layer (e.g. Nginx `limit_req_zone $binary_remote_addr zone=login:10m rate=5r/m;`).

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
