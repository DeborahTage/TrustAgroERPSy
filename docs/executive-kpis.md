# Executive KPIs (Step 11)

Read-only executive layer. Finance is the financial source of truth.
**INCOME = revenue. TRANSFER payments settle AR/AP and are never revenue.**

Date convention: inclusive `from`/`to`. Presets resolve via `ExecutiveDateRange`.

| KPI | Source module | Calculation | Date basis | Exclusions | Authority |
|-----|---------------|-------------|------------|------------|-----------|
| totalRevenue | Finance P&L / INCOME | `FinanceReportService.getProfitAndLoss` or module-filtered `sumByTypeDateRangeAndModule(INCOME)` | transactionDate inclusive | TRANSFER payments, unpaid AR increases | `executive:finance` or `finance:read` |
| totalExpenses | Finance P&L / EXPENSE | P&L expenses or module-filtered EXPENSE sum | transactionDate inclusive | Fake zero COGS when missing | `executive:finance` or `finance:read` |
| netProfit | Finance | totalRevenue − totalExpenses | period | Incomplete cost allocations → treat carefully / INCOMPLETE | same |
| revenueByModule | Finance | `sumGroupedByModule(INCOME)` | period | Never TRANSFER | same |
| expenseByModule | Finance | `sumGroupedByModule(EXPENSE)` | period | — | same |
| cashInHand / bankBalance | Finance CoA 1000 / 1010 | Point-in-time `ChartOfAccount.balance` | as-of now | Not period revenue | same |
| cashInflows / cashOutflows | Finance | `FinanceReportService.getCashFlow` (cash/bank 1000/1010 only) | period | Invoice issuance / AR increase | same |
| accountsReceivable | Finance invoices | `sumOutstandingOpen` (SENT / PARTIALLY_PAID / OVERDUE) | as-of now | Not revenue | same |
| overdue AR | Finance invoices | dueDate &lt; today and open statuses | as-of now | Customer aging buckets UNAVAILABLE | same |
| accountsPayable | Finance CoA 2000 | CoA balance; aging via Procurement when allowed | as-of now | Aging UNAVAILABLE without `procurement:read` / `executive:finance` | finance / procurement gates |
| conversionRate | CRM | convertedLeads / totalLeads × 100 | lifetime leads | Never invented | `executive:sales` / `crm:read` / `crm:report` |
| CRM invoiced / paid / AR | CRM + Finance invoices | commercialDashboard finance invoice sums | as-of / lifetime | — | same |
| activeFarms / flocks | Farm | `countByStatus(ACTIVE)` | as-of now | — | `executive:farm` / `farm:read` |
| mortalityInRange | Farm daily records | `sumMortalityBetween` | period | — | same |
| FCR | Farm | `calculateFCR` returns null | n/a | Always **UNAVAILABLE** | same |
| inventory value / low / expiring | Inventory | `getDashboardStats` subset | as-of now | — | `executive:inventory` / `inventory:read` |
| procurement open PO / AP | Procurement | `dashboard()` subset | as-of now | — | procurement / executive finance or inventory |
| consultancy / training revenue | Consultancy / Training | module dashboard Finance INCOME fields | module-defined | — | consultancy/training read or `executive:sales` |
| workforce headcount / payroll totals | HR | `HrService.dashboard` aggregates only | as-of / current payroll | No individual salaries/documents | `executive:hr` or `hr:report` / `hr:read` |
| alerts | Cross-module | Rule-based from real conditions only | as-of / today | No fake alerts | `executive:dashboard` |

## Trend tradeoff

`FinanceTransactionRepository.sumByTypeGroupedByYearMonth` uses JPQL `FUNCTION('YEAR'|'MONTH')`.
Executive revenue/expense trends use portable per-month `sumByTypeDateRangeAndModule` loops so H2 (`MODE=PostgreSQL`) and PostgreSQL both work without dialect surprises.

## Incomplete / unavailable policy

Never substitute `0` for missing cost, FCR, or aging buckets. Emit `status: UNAVAILABLE` or `INCOMPLETE` instead.
