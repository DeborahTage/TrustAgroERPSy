# Enterprise Controls (Step 12)

Trust Agro ERP uses a single application audit trail (`AuditService` / `AuditLog`) plus the existing
ledger-specific `FinanceAuditLog` for finance postings. Do not introduce parallel audit systems.

## Audit policy

- Audit writes are **synchronous** and participate in the **caller transaction**. A business rollback
  also rolls back the corresponding audit row (avoids false SUCCESS trails).
- Sensitive payload keys are redacted before save: `password`, `token`, `jwt`, `secret`, `otp`
  (and common variants). Passwords must never be stored in audit values.
- Unauthenticated events (e.g. failed login) use `logSystem(...)` with the **attempted email**, never
  the password.
- Optional fields: `result` (`SUCCESS`/`FAILURE`), `ipAddress` (when an HTTP request is present).
- Index `idx_audit_module` supports module-filtered search.

### Retention

- Audit logs are **retained indefinitely** in the application database.
- There is **no automatic delete/purge** job and the Audit API exposes **no delete/update** endpoints.
- Operational archival (if needed) is an offline DBA process outside the application.

### Security events

`GET /api/audit-logs/security-events` surfaces `LOGIN_*`, `LOGOUT`, `PASSWORD_CHANGE`, `ROLE_CHANGE`,
`USER_DISABLED`, `PERMISSION`, and `module=SECURITY`.

## Segregation of duties (self-approval)

Where the creator/requester is known, the same user cannot approve:

| Document | Field compared | Message |
|----------|----------------|---------|
| Purchase order | `createdBy` | Cannot self-approve a purchase order |
| Supplier invoice | `createdBy` | Cannot self-approve a supplier invoice |
| CRM quotation | `createdBy` | Cannot self-approve a quotation |
| Internal / proc requisition | `requestedBy` | Cannot self-approve a requisition |

Leave and payroll already enforce analogous self-approval rules in HR.

## Login rate limit

- After **5** failed attempts for the same email (or the same client IP) within **15 minutes**,
  login returns `BusinessException`: `Too many login attempts. Try again later.`
- Counter is in-memory per JVM (resets on restart). Failed attempts are audited as `LOGIN_FAILED`.
- Inactive accounts are rejected via `CustomUserDetailsService` (`enabled=false`) with the same
  generic client message as bad credentials: `Invalid email or password`.

## Soft-delete master data

- Master data `delete` and supplier `delete` **deactivate** (`active=false`) and audit `DEACTIVATE`.
  Hard deletes are refused for these master records.

## Governance API

- `GET /api/governance/dashboard` and `GET /api/governance/controls`
- Authorities: `audit:read`, `executive:dashboard`, `reports:read`, or `governance:read`
- `governance:read` is granted to ADMIN, GENERAL_MANAGER, and FINANCE.

## Logout / password change

- `POST /api/auth/change-password` — authenticated; verifies current password with BCrypt; audits
  `PASSWORD_CHANGE` without storing secrets.
- `POST /api/auth/logout` — audits `LOGOUT`. JWT is **not** revoked server-side; clients must discard
  the token.
