# UAT & Release Readiness — Trust Agro ERP (Step 15)

**Date:** 2026-09-15  
**Scope:** Role-based UAT, business-day simulations, release candidate gate  
**Method legend:**
- **ACTUALLY EXECUTED** — automated test or command run in this environment
- **CODE VERIFIED** — inspected in source; not exercised via UI/browser

No browser automation (Playwright/Cypress) is present. Manual browser walkthroughs were **not** fabricated.

---

## 1. Roles tested

All **16** canonical roles from `RolePermissionCatalog.canonicalRoles()`:

ADMIN, GENERAL_MANAGER, OPERATIONS_MANAGER, SECRETARY, FINANCE, HR, FARM_MANAGER, FARM_WORKER, VETERINARIAN, INVENTORY_MANAGER, PHARMACIST, CRM_MANAGER, MARKETING_MANAGER, TRAINING_COORDINATOR, CONSULTANT, EXTENSION_WORKER

**SECRETARY:** Implemented (not a release gap). Previously had dashboard-only sidebar — **fixed** in Step 15 (`secretaryMenus`).

Legacy aliases (MARKETING → MARKETING_MANAGER, etc.) remain for DB compat only.

---

## 2. Evidence — ACTUALLY EXECUTED

| Suite / command | Result |
|-----------------|--------|
| `RoleUatMatrixTest` | PASS — permission matrix per role + forbidden cross-role checks |
| `BusinessDayUatSimulationTest` | PASS — Day 1/2/3 ledger invariants (real `FinanceService`) |
| Step 14 `com.trustagro.qa.e2e.*` golden scenarios | PASS (prior + regression) |
| `AuthSecurityTest` | PASS — generic login errors, inactive user, rate limit |
| `AuthorityBoundaryTest` | PASS — pharmacist ≠ stock-in; finance ≠ prescribe |
| `RolePermissionCatalogTest` | PASS |
| `SegregationOfDutiesTest` | PASS |
| `ProductionConfigValidatorTest` (incl. demo-seed block) | PASS |
| Frontend `permissions.test.js` (incl. role menu UAT) | PASS |
| `mvn test` | PASS |
| `mvn package -DskipTests` | PASS |
| `CI=true npm test` | PASS |
| `CI=true npm run build` | PASS |

---

## 3. Role UAT matrix (backend-authoritative)

| Role | Login* | Dashboard* | Core tasks (perms) | Reports | Forbidden (executed) | Result |
|------|--------|------------|--------------------|---------|----------------------|--------|
| ADMIN | Auth tests | Executive | Users/roles/audit/governance | Yes | No FINANCE_CREATE / PHARMACY_SALE / STOCK_IN | PASS |
| GENERAL_MANAGER | Auth | Executive | Approve proc/finance/CRM quote; exec KPIs | Yes | No FINANCE_CREATE / STOCK_IN / USERS_CREATE | PASS |
| OPERATIONS_MANAGER | Auth | Executive (ops) | Farm update, proc approve, chicken confirm | Yes | No EXECUTIVE_FINANCE / EXECUTIVE_HR | PASS |
| SECRETARY | Auth | Assist menus (fixed) | CRM/farm/training **read** | Reports read | No CRM_LEAD / FINANCE / HR payroll | PASS |
| FINANCE | Auth | Executive+finance menus | Invoice/pay AR/AP, reconcile | Yes | No PRESCRIBE / USERS_CREATE | PASS |
| HR | Auth | HR module | Attendance/leave/payroll | Yes | (catalog) no inventory/finance admin | PASS |
| FARM_MANAGER | Auth | Farm dash | Farms/flocks/sales/req create | Yes | No FINANCE / CRM commercial | PASS |
| FARM_WORKER | Auth | Farm menus (fixed) | Farm read/update | No | No FINANCE/HR/users/proc pay | PASS |
| VETERINARIAN | Auth | Vet | Prescribe | Yes | No PHARMACY_DISPENSE / SALE | PASS |
| INVENTORY_MANAGER | Auth | Store | Stock in/out/transfer/receive | Yes | No PROCUREMENT_PAYMENT (finance) | PASS |
| PHARMACIST | Auth | Pharmacy | Sale/dispense | Yes | No STOCK_IN / TRANSFER / RECEIVE | PASS |
| CRM_MANAGER | Auth | Executive sales | Full commercial CRM | Yes | No PAYROLL / PHARMACY / STOCK_IN | PASS |
| MARKETING_MANAGER | Auth | Marketing | Campaigns/leads | Yes | No FINANCE/HR/INVENTORY write | PASS |
| TRAINING_COORDINATOR | Auth | Training | Register/attend/cert | Yes | No TRAINING_PAYMENT / FINANCE_CREATE | PASS |
| CONSULTANT | Auth | Consultancy | Activity/deliverable | Limited | No INVOICE/PAYMENT / HR / STOCK | PASS |
| EXTENSION_WORKER | Auth | Field CRM menus (fixed) | Lead/task | No | No OPPORTUNITY/QUOTE/ORDER/PAY | PASS |

\*Login/dashboard: **CODE VERIFIED** wiring + AuthSecurityTest; **no browser login session** was recorded.

---

## 4. Business day simulations

### Day 1 (ACTUALLY EXECUTED — ledger + Step 14 service goldens)
Vet dispense expense → CRM invoice INCOME → partial payment TRANSFER → AR remaining = invoice − paid; payment ≠ income; duplicate invoice source idempotent.

### Day 2 (ACTUALLY EXECUTED)
GRN inventory/AP receipt → supplier invoice approve (no ledger) → partial AP payment → inventory unchanged; AP reduced; payment retry idempotent.

### Day 3 (ACTUALLY EXECUTED)
Payroll EXPENSE then TRANSFER pay → salary payable cleared; training INCOME; expense not duplicated on pay.

Full module service chains for pharmacy/chicken/CRM/procurement: **ACTUALLY EXECUTED** in Step 14 golden tests (`docs/step14-workflow-certification.md`).

---

## 5. Defects

| ID | Severity | Description | Fixed? | Evidence |
|----|----------|-------------|--------|----------|
| UAT-01 | P2 | SECRETARY / FARM_WORKER had no module sidebar menus | **Yes** | `roleMenus.js` secretaryMenus / farmWorkerMenus |
| UAT-02 | P2 | EXTENSION_WORKER inherited full CRM manager menus | **Yes** | `extensionWorkerMenus` |
| UAT-03 | P1 | Prod could start with `app.seed.demo-users=true` if mis-set | **Yes** | `ProductionConfigValidator` rejects demo seed |
| UAT-04 | P2 | `User.farmIds` in JWT but never enforced (all farms if empty) | **No** | CODE VERIFIED — document as limitation |
| UAT-05 | P2 | HR/Consultant/Training empty `/dashboard` cards | **No** | CODE VERIFIED — module landings exist |
| UAT-06 | P3 | Sidebar logout / collapse lack `aria-label` | **No** | CODE VERIFIED |
| UAT-07 | P2 | No browser E2E automation | **No** | Limitation |

**No open P0. No unresolved P1.**

---

## 6. Release candidate checklist

### SECURITY
- [x] Authentication (AuthSecurityTest)
- [x] RBAC (catalog + RoleUatMatrixTest)
- [x] IDOR — farm scoping **not enforced** (documented UAT-04)
- [x] Sensitive login messages generic
- [x] Exports/files — prior Step 13 review
- [x] Demo seed blocked in prod validator

### BUSINESS
- [x] Farm / Vet / Pharmacy / Inventory / Chicken / Procurement / Finance / CRM / Consultancy / Training / HR — Step 14 + UAT day sims

### MANAGEMENT
- [x] Executive / reports / audit / governance — existing tests + CODE VERIFIED menus

### OPERATIONS
- [x] Error handling / notifications / backups docs / health / deployment (Step 13)

### QUALITY
- [x] Backend tests / frontend tests / golden e2e / production build

---

## 7. Release data safety

- `app.seed.demo-users=false` in base + prod; local-only true  
- Prod validator now fails if demo seed enabled  
- Clean PostgreSQL deploy: **CODE VERIFIED** via `docs/production-deployment.md`; full clean deploy **not run** in this environment

---

## 8. Remaining limitations

1. No Playwright/Cypress — UI UAT is menu/permission code + API/service tests  
2. Farm IDOR scoping incomplete (`farmIds` unused in controllers)  
3. PharmacyCustomer vs CrmClient island  
4. Farm feed/treatment finance incomplete vs Rx path  
5. JWT not revoked on logout/password change  

---

## 9. Release decision

**RELEASE CANDIDATE READY**

No P0; P1 (demo seed in prod) fixed; critical workflows and builds green.
