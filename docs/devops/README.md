# DevOps and CI guide

This guide describes the current CI foundation and the checks to complete before
any staging or production deployment. CI does not deploy the application.

## Local test and build commands

Prerequisites: Java 17, Maven 3.8+, Node.js 24 LTS, and npm. `react-router-dom`
7.14.2 declares Node.js `>=20.0.0`; CI uses Node 24. Use the committed
`frontend/package-lock.json` with `npm ci` so local installs match CI.

From PowerShell at the repository root:

```powershell
Push-Location backend
mvn -B test package
Pop-Location

Push-Location frontend
npm ci
$env:CI = "true"
npm test -- --watchAll=false --coverage
npm run build
Remove-Item Env:\CI
Pop-Location
```

The backend tests use the `test` profile and an in-memory H2 database. The
frontend test and build commands run in CI mode so failures return a non-zero
exit code instead of waiting for interactive input.

## GitHub Actions

`.github/workflows/ci.yml` runs on pushes to branches and on pull requests
targeting `main`. The workflow:

1. Sets up Java 17 and Node 24.
2. Runs `mvn -B test package` in `backend`.
3. Installs locked frontend dependencies with `npm ci`, runs the tests, and
   creates a production build.
4. Validates the Compose file and builds the backend and frontend images.
5. Uploads backend Surefire reports and frontend coverage when available,
   including when a later CI step fails.

The Compose checks use clearly non-production placeholder values for required
Compose interpolation only. The workflow does not read GitHub secrets, publish
images, or deploy. A failed step is a failed check; test failures are not
ignored.

To diagnose a failed run, open the failed job and read the first failing step's
log. Backend JUnit XML and frontend coverage are available under the run's
**Artifacts** when those files were generated. Fix the cause and push a new
commit; do not bypass or mark a failing check successful.

The workflow currently runs pushes on every branch because the repository's
development branch could not be verified from this source directory. Pull
requests are gated to `main`. Once the real Git repository and development
branch are confirmed, narrow push triggers to the agreed branch names if
desired.

## Environment variables and secrets

Production requires these variables (names only; never commit their values):

- `SPRING_PROFILES_ACTIVE` — set to `prod` by deployment tooling; the app's
  default is also `prod`.
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `APP_JWT_SECRET` — at least 32 characters.
- `APP_CORS_ALLOWED_ORIGINS` — explicit comma-separated origins, not a sole
  wildcard.

Optional/runtime or build-time settings include `SERVER_PORT`, `UPLOAD_DIR`,
`POSTGRES_PORT`, `BACKEND_PORT`, `FRONTEND_PORT`, and `REACT_APP_API_URL`.
`REACT_APP_API_URL` is embedded in the frontend at build time.

Use a local `.env` file or a deployment secret manager for credentials. The
root `.env.example` contains placeholders and configuration examples only.
Local `.env*` files, token/log files, and common private-key formats are
gitignored. The production validator fails startup for missing JWT, database,
or CORS settings and rejects demo seeding. Do not put credentials in workflow
YAML, Docker build arguments, logs, or frontend variables.

## Health and logs

The backend exposes `/actuator/health` and `/actuator/info`; only these actuator
endpoints are enabled for web exposure. Health is permitted without JWT for
container probes, while production config sets detail visibility to
`when_authorized`. The nginx frontend returns 404 for `/actuator/`. Application
logs go to container stdout; inspect them with
`docker compose logs --tail 100 backend`. Production uses WARN root logging
and INFO for application classes.

## Docker Compose

From the repository root, after preparing a local `.env` with real local-only
values:

```powershell
docker compose config --quiet
docker compose build
```

`config --quiet` validates interpolation without printing the resolved
configuration. Image builds require `backend/target/trust-agro-backend-1.0.0.jar`
and `frontend/build/`, so run the backend package and frontend production build
commands first. Compose starts PostgreSQL 15, the production-profile backend,
and nginx; `docker compose build` does not start services.

## Known deployment and database risks

- There are no Flyway or Liquibase migrations in the current backend. Local
  H2 uses Hibernate `update`; production PostgreSQL uses `validate` and will
  not create or migrate tables. A DBA-reviewed schema bootstrap/release process
  is required before a new staging database can run the application.
- The production deployment guide documents a one-time bootstrap using
  Hibernate `update`. Treat that as a controlled, reviewed operation—not a
  repeatable migration system—and verify the resulting schema before normal
  `validate` startup.
- Compose persists PostgreSQL and uploads in named volumes. The project
  documents `pg_dump` and restore verification, but automated scheduled backups,
  off-site retention, and restore drills are not provided by this CI workflow;
  see the production deployment guide.
- Compose is a single-host reference stack, not a high-availability or
  externally secured ingress design. TLS, firewalling, secret-manager
  integration, database monitoring, and operational alerting still need an
  environment-specific design.
- A successful build does not establish release readiness. Test coverage,
  staging data migration, rollback, and business acceptance remain separate
  gates.

## Before staging or production

1. Obtain the actual Git clone, inspect its branch and worktree, preserve any
   existing work, and checkpoint these changes on a reviewed feature branch.
2. Confirm the development branch with the project owner and protect `main`
   with required CI checks and pull-request review.
3. Provision a non-production PostgreSQL database and secret store. Create and
   review the schema using an explicit staging procedure; do not rely on
   Hibernate `update` as an ongoing migration strategy.
4. Test a fresh staging deployment, health/readiness checks, authentication,
   representative read-only workflows, backup creation, and restore to a
   separate database. Verify logs and rollback steps.
5. Agree on an image/artifact versioning and promotion process before any
   production deployment. Production deployment automation is intentionally
   out of scope here.
