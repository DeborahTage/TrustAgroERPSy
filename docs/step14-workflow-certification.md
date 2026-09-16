# Step 14 — ERP Workflow Map & QA Certification Notes

Traced from actual services in this repository (not aspirational).

## Test strategy

| Level | Approach in this repo |
|-------|------------------------|
| Unit | Lifecycle/rules tests, Mockito service tests |
| Service / cross-module | Golden scenarios under `com.trustagro.qa.e2e` with **real** `FinanceService` (`QaFinanceHarness`) |
| Controller/API | Annotation/authority contract tests; no full MockMvc suite |
| Browser E2E | **Not present** (no Playwright/Cypress) — documented limitation |

Test DB: H2 `create-drop` profile `test`. No `@SpringBootTest` (fixtures are reproducible via mocks + real Finance ledger).

---

## Workflow map (actual)

### A. Procurement
`ProcurementService`: requisition → approve (SoD) → PO → GRN create → **`postGoodsReceipt` → `InventoryService.stockIn` → `FinanceService.postInventoryReceipt` (Dr Inventory / Cr AP)** → supplier invoice match/approve (**no finance**) → `createPayment` → `TRANSFER` Dr AP / Cr Cash|Bank.

### B. Farm
Flock/daily records/feed silos are operational. Prescription dispense posts farm cost allocation; direct `VeterinaryService.createTreatment` stock-out **without** finance allocation (gap).

### C. Veterinary → Pharmacy
`PrescriptionWorkflowService.dispense` → inventory stock-out → `postInventoryConsumption` + `FarmCostAllocation`. Duplicate dispense blocked by status.

### D. Pharmacy POS
`PharmacyService.processSale` → stockOut once → Invoice + INCOME revenue + VAT TRANSFER + COGS. Cash settles at sale; CREDIT updates `PharmacyCustomer` AR island.

### E. Chicken sales
`ChickenSaleService`: confirm (flock decrement) → invoice (INCOME) → payment (TRANSFER). Customer = `CrmClient`.

### F. CRM product order
Lead → convert → opportunity → quotation → accept → `CustomerOrderService` → invoice INCOME → payment TRANSFER.

### G. CRM → Consultancy
Accept consultancy quotation → request/engagement/contract DRAFT → manual activate → `createEngagementInvoice` INCOME → payment TRANSFER.

### H. Training
`confirmRegistration` posts INCOME; `recordPayment` is TRANSFER only.

### I. HR payroll
`processPayroll` EXPENSE accrual; `payPayroll` TRANSFER settlement only. SoD on self-approve.

### J. Executive
Reads Finance INCOME aggregates, invoice AR, procurement AP, module dashboards — must not treat TRANSFER payments as revenue (`ExecutiveDoubleCountingTest`).

---

## Finance matrix

| Module | Revenue source | Invoice | Payment | AR/AP | Double-count safe |
|--------|----------------|---------|---------|-------|-------------------|
| CRM orders | INCOME on invoice | Yes | TRANSFER | AR | Yes (source keys) |
| Chicken | INCOME on invoice | Yes | TRANSFER | AR | Yes |
| Pharmacy | INCOME on sale | Yes | Cash at sale / credit balance | PharmacyCustomer | Yes if POS path only |
| Consultancy | INCOME on engagement invoice | Yes | TRANSFER | AR | Yes |
| Training | INCOME on confirm | Yes | TRANSFER | AR | Yes |
| Procurement | none (AP on GRN) | Supplier inv document | TRANSFER settles AP | AP | Yes |
| Vet dispense | none (EXPENSE consumption) | n/a | n/a | n/a | Yes |
| Payroll | EXPENSE on process | n/a | TRANSFER on pay | Salary payable | Yes |

---

## Customer islands

- **Canonical commercial:** `CrmClient`
- **Pharmacy POS:** `PharmacyCustomer` (not linked)
- **Marketing:** `MarketingLead` → `CrmClient` (not `CrmLead`)

## Inventory

All SKU mutations via `InventoryService`. Feed silos and flock bird counts are separate. Cycle count UI does not persist adjustments.

## Browser E2E

Not introduced in Step 14. Cross-module proof is service-level golden scenarios + existing module tests.
