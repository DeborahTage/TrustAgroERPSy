package com.trustagro.executive;

import com.trustagro.consultancy.service.ConsultancyService;
import com.trustagro.crm.service.CrmCommercialService;
import com.trustagro.farm.entity.FarmStatus;
import com.trustagro.farm.entity.FlockStatus;
import com.trustagro.farm.repository.DailyFarmRecordRepository;
import com.trustagro.farm.repository.FarmRepository;
import com.trustagro.farm.repository.FlockRepository;
import com.trustagro.farm.service.FlockManagementService;
import com.trustagro.finance.entity.AccountCode;
import com.trustagro.finance.entity.FinanceSourceTypes;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.repository.ChartOfAccountRepository;
import com.trustagro.finance.repository.FinanceTransactionRepository;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.finance.service.FinanceReportService;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.governance.GovernanceService;
import com.trustagro.hr.service.HrService;
import com.trustagro.inventory.service.InventoryService;
import com.trustagro.pharmacy.service.PharmacyService;
import com.trustagro.procurement.service.ProcurementService;
import com.trustagro.security.Permissions;
import com.trustagro.training.service.TrainingService;
import com.trustagro.veterinary.entity.PrescriptionStatus;
import com.trustagro.veterinary.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Read-only executive aggregation. Finance ledger is the financial source of truth;
 * INCOME = revenue; TRANSFER payments are never counted as revenue.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExecutiveService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String STATUS = "status";
    private static final String AVAILABLE = "AVAILABLE";
    private static final String UNAVAILABLE = "UNAVAILABLE";
    private static final String INCOMPLETE = "INCOMPLETE";

    private final FinanceService financeService;
    private final FinanceReportService financeReportService;
    private final FinanceTransactionRepository transactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final CrmCommercialService crmCommercialService;
    private final FarmRepository farmRepository;
    private final FlockRepository flockRepository;
    private final DailyFarmRecordRepository dailyFarmRecordRepository;
    private final FlockManagementService flockManagementService;
    private final InventoryService inventoryService;
    private final PharmacyService pharmacyService;
    private final ProcurementService procurementService;
    private final ConsultancyService consultancyService;
    private final TrainingService trainingService;
    private final HrService hrService;
    private final PrescriptionRepository prescriptionRepository;
    private final GovernanceService governanceService;

    @Value("${app.alerts.mortality-threshold:1.0}")
    private double mortalityAlertThreshold;

    public Map<String, Object> dashboard(String preset, LocalDate from, LocalDate to, String businessLine) {
        ExecutiveDateRange range = ExecutiveDateRange.resolve(preset, from, to);
        String moduleFilter = normalizeBusinessLine(businessLine);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("period", range.toMap());
        payload.put("businessLine", moduleFilter == null ? "ALL" : moduleFilter);
        payload.put("definitions", ExecutiveDefinitions.all());

        if (canSeeFinance()) {
            payload.put("finance", financialSummary(range, moduleFilter));
        }
        if (canSeeSales()) {
            payload.put("sales", sales(range, moduleFilter));
        }
        if (canSeeFarm()) {
            payload.put("farm", farm(range));
        }
        if (canSeeInventory()) {
            payload.put("inventory", inventory());
        }
        if (canSeeProcurement()) {
            payload.put("procurement", procurement());
        }
        if (canSeeServices()) {
            payload.put("services", services());
        }
        if (canSeeWorkforce()) {
            payload.put("workforce", workforce());
        }
        if (hasAny(Permissions.EXECUTIVE_DASHBOARD)) {
            payload.put("alerts", alerts(range));
        }
        return payload;
    }

    public Map<String, Object> financialSummary(String preset, LocalDate from, LocalDate to, String businessLine) {
        return financialSummary(ExecutiveDateRange.resolve(preset, from, to), normalizeBusinessLine(businessLine));
    }

    Map<String, Object> financialSummary(ExecutiveDateRange range, String moduleFilter) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("period", range.toMap());

        Map<String, Object> pl = financeReportService.getProfitAndLoss(range.from(), range.to());
        Map<String, Object> balances = financeService.getDashboardSummary();
        Map<String, Object> cashFlow = financeReportService.getCashFlow(range.from(), range.to());

        BigDecimal totalRevenue = nvl((BigDecimal) pl.get("totalRevenue"));
        BigDecimal totalExpenses = nvl((BigDecimal) pl.get("totalExpenses"));
        BigDecimal netProfit = nvl((BigDecimal) pl.get("netProfit"));

        Map<String, BigDecimal> revenueByModule = toAmountMap(
                transactionRepository.sumGroupedByModule(TransactionType.INCOME, range.from(), range.to()));
        Map<String, BigDecimal> expenseByModule = toAmountMap(
                transactionRepository.sumGroupedByModule(TransactionType.EXPENSE, range.from(), range.to()));

        if (moduleFilter != null) {
            totalRevenue = nvl(transactionRepository.sumByTypeDateRangeAndModule(
                    TransactionType.INCOME, range.from(), range.to(), moduleFilter));
            totalExpenses = nvl(transactionRepository.sumByTypeDateRangeAndModule(
                    TransactionType.EXPENSE, range.from(), range.to(), moduleFilter));
            netProfit = totalRevenue.subtract(totalExpenses);
            revenueByModule = filterModule(revenueByModule, moduleFilter);
            expenseByModule = filterModule(expenseByModule, moduleFilter);
        }

        out.put("totalRevenue", totalRevenue);
        out.put("totalExpenses", totalExpenses);
        out.put("netProfit", netProfit);
        out.put(STATUS, AVAILABLE);
        out.put("cashInHand", balances.get("cashInHand"));
        out.put("bankBalance", balances.get("bankBalance"));
        out.put("accountsReceivable", balances.get("accountsReceivable"));
        out.put("accountsPayable", balances.get("accountsPayable"));
        out.put("cashInflows", cashFlow.get("cashInflows"));
        out.put("cashOutflows", cashFlow.get("cashOutflows"));
        out.put("netCashChange", cashFlow.get("netCashChange"));
        out.put("revenueByModule", revenueByModule);
        out.put("expenseByModule", expenseByModule);
        out.put("knownCogs", pl.get("knownCogs"));
        out.put("limitationNote", pl.get("limitationNote"));
        out.put("accountingNote",
                "Revenue is recognized on INCOME postings (invoice/sale). "
                        + "TRANSFER payments settle AR/AP and are never additional revenue. "
                        + "Cash/bank/AR/AP balances are point-in-time CoA/invoice figures.");
        out.put("profitabilityStatus",
                pl.get("knownCogs") != null && ((BigDecimal) pl.get("knownCogs")).compareTo(ZERO) == 0
                        && totalExpenses.compareTo(ZERO) == 0 && totalRevenue.compareTo(ZERO) > 0
                        ? INCOMPLETE : AVAILABLE);
        // Monthly trend via portable per-month sums (avoids dialect FUNCTION YEAR/MONTH issues on some H2 setups)
        out.put("revenueTrend", monthlyTrend(TransactionType.INCOME, range.from(), range.to(), moduleFilter));
        out.put("expenseTrend", monthlyTrend(TransactionType.EXPENSE, range.from(), range.to(), moduleFilter));
        out.put("trendNote",
                "Trends use per-month sumByTypeDateRangeAndModule for portability across H2/PostgreSQL; "
                        + "FinanceTransactionRepository.sumByTypeGroupedByYearMonth remains available for SQL GROUP BY.");
        return out;
    }

    public Map<String, Object> revenue(String preset, LocalDate from, LocalDate to, String businessLine) {
        Map<String, Object> fin = financialSummary(preset, from, to, businessLine);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("period", fin.get("period"));
        out.put("totalRevenue", fin.get("totalRevenue"));
        out.put("revenueByModule", fin.get("revenueByModule"));
        out.put("revenueTrend", fin.get("revenueTrend"));
        out.put("accountingNote", fin.get("accountingNote"));
        out.put(STATUS, AVAILABLE);
        return out;
    }

    public Map<String, Object> expenses(String preset, LocalDate from, LocalDate to, String businessLine) {
        Map<String, Object> fin = financialSummary(preset, from, to, businessLine);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("period", fin.get("period"));
        out.put("totalExpenses", fin.get("totalExpenses"));
        out.put("expenseByModule", fin.get("expenseByModule"));
        out.put("expenseTrend", fin.get("expenseTrend"));
        out.put("knownCogs", fin.get("knownCogs"));
        out.put("limitationNote", fin.get("limitationNote"));
        out.put(STATUS, AVAILABLE);
        return out;
    }

    public Map<String, Object> cashFlow(String preset, LocalDate from, LocalDate to) {
        ExecutiveDateRange range = ExecutiveDateRange.resolve(preset, from, to);
        Map<String, Object> cf = financeReportService.getCashFlow(range.from(), range.to());
        Map<String, Object> balances = financeService.getDashboardSummary();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("period", range.toMap());
        out.put("cashInflows", cf.get("cashInflows"));
        out.put("cashOutflows", cf.get("cashOutflows"));
        out.put("netCashChange", cf.get("netCashChange"));
        out.put("cashInHand", balances.get("cashInHand"));
        out.put("bankBalance", balances.get("bankBalance"));
        out.put("note", cf.get("note"));
        out.put(STATUS, AVAILABLE);
        return out;
    }

    public Map<String, Object> receivables(String preset, LocalDate from, LocalDate to) {
        ExecutiveDateRange range = ExecutiveDateRange.resolve(preset, from, to);
        LocalDate today = LocalDate.now();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("period", range.toMap());
        out.put("outstandingAr", nvl(invoiceRepository.sumOutstandingOpen()));
        out.put("openInvoiceCount", invoiceRepository.countOpen());
        out.put("overdueCount", invoiceRepository.countOverdueOpen(today));
        out.put("overdueOutstanding", nvl(invoiceRepository.sumOverdueOutstanding(today)));
        Map<String, Object> aging = new LinkedHashMap<>();
        aging.put(STATUS, UNAVAILABLE);
        aging.put("note", "Customer aging buckets by days-past-due are not implemented; overdue total is available.");
        out.put("agingBuckets", aging);
        out.put(STATUS, AVAILABLE);
        out.put("note", "AR is open invoice outstanding (SENT/PARTIALLY_PAID/OVERDUE). Partial payments reduce outstanding.");
        return out;
    }

    public Map<String, Object> payables(String preset, LocalDate from, LocalDate to) {
        ExecutiveDateRange range = ExecutiveDateRange.resolve(preset, from, to);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("period", range.toMap());
        BigDecimal ap = chartOfAccountRepository.findByAccountCode(AccountCode.LIABILITY_AP.getCode())
                .map(a -> nvl(a.getBalance())).orElse(ZERO);
        out.put("accountsPayable", ap);
        out.put(STATUS, AVAILABLE);

        if (hasAny(Permissions.PROCUREMENT_READ, Permissions.EXECUTIVE_FINANCE)) {
            try {
                Map<String, Object> dash = procurementService.dashboard();
                Map<String, Object> proc = new LinkedHashMap<>();
                proc.put("approvedUnpaidInvoices", dash.get("approvedUnpaidInvoices"));
                proc.put("apOutstanding", dash.get("apOutstanding"));
                proc.put("invoicesAwaitingMatch", dash.get("invoicesAwaitingMatch"));
                out.put("procurement", proc);
                out.put("apAging", procurementService.apAging());
                out.put("agingStatus", AVAILABLE);
            } catch (Exception ex) {
                out.put("agingStatus", UNAVAILABLE);
                Map<String, Object> unavailable = new LinkedHashMap<>();
                unavailable.put(STATUS, UNAVAILABLE);
                unavailable.put("note", "Procurement aging unavailable");
                out.put("apAging", unavailable);
            }
        } else {
            out.put("agingStatus", UNAVAILABLE);
            Map<String, Object> unavailable = new LinkedHashMap<>();
            unavailable.put(STATUS, UNAVAILABLE);
            unavailable.put("note", "Requires procurement:read or executive:finance");
            out.put("apAging", unavailable);
        }
        return out;
    }

    public Map<String, Object> sales(String preset, LocalDate from, LocalDate to, String businessLine) {
        return sales(ExecutiveDateRange.resolve(preset, from, to), normalizeBusinessLine(businessLine));
    }

    Map<String, Object> sales(ExecutiveDateRange range, String moduleFilter) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("period", range.toMap());
        Map<String, Object> commercial = crmCommercialService.commercialDashboard();
        out.put("pipelineLeads", commercial.get("pipelineLeads"));
        out.put("opportunitiesByStage", commercial.get("opportunitiesByStage"));
        out.put("openOpportunityValue", commercial.get("openOpportunityValue"));
        out.put("quotationsByStatus", commercial.get("quotationsByStatus"));
        out.put("ordersByStatus", commercial.get("ordersByStatus"));
        out.put("conversionRate", commercial.get("conversionRate"));
        out.put("crmInvoiced", commercial.get("totalOrderRevenueInvoiced"));
        out.put("crmPaid", commercial.get("totalOrderPaid"));
        out.put("crmOutstandingAr", commercial.get("outstandingAr"));
        out.put("openTasks", commercial.get("openTasks"));

        BigDecimal crmIncome = nvl(transactionRepository.sumByTypeDateRangeAndModule(
                TransactionType.INCOME, range.from(), range.to(), FinanceSourceTypes.MODULE_CRM));
        out.put("crmPeriodIncome", crmIncome);

        if (moduleFilter == null || FinanceSourceTypes.MODULE_CRM.equals(moduleFilter)
                || FinanceSourceTypes.MODULE_CHICKEN_SALE.equals(moduleFilter)
                || FinanceSourceTypes.MODULE_PHARMACY.equals(moduleFilter)) {
            out.put(STATUS, AVAILABLE);
        } else {
            out.put(STATUS, AVAILABLE);
            out.put("filterNote", "Commercial CRM metrics are global; period income filtered when businessLine=CRM");
        }

        try {
            Map<String, Object> pharmacy = pharmacyService.getDashboardStats();
            out.put("pharmacyRevenueToday", pharmacy.get("revenue_today"));
            out.put("pharmacySalesCountToday", pharmacy.get("sales_count_today"));
        } catch (Exception ignored) {
            // pharmacy optional for sales view
        }
        return out;
    }

    public Map<String, Object> farm(String preset, LocalDate from, LocalDate to) {
        return farm(ExecutiveDateRange.resolve(preset, from, to));
    }

    Map<String, Object> farm(ExecutiveDateRange range) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("period", range.toMap());
        out.put("activeFarms", farmRepository.countByStatus(FarmStatus.ACTIVE));
        out.put("activeFlocks", flockRepository.countByStatus(FlockStatus.ACTIVE));
        Integer mortality = dailyFarmRecordRepository.sumMortalityBetween(range.from(), range.to());
        Integer todayMortality = dailyFarmRecordRepository.sumMortalityByDate(LocalDate.now());
        out.put("mortalityInRange", mortality != null ? mortality : 0);
        out.put("todayMortality", todayMortality != null ? todayMortality : 0);
        // FCR is intentionally null from farm service — never fake a number
        Double fcr = flockManagementService.calculateFCR(null);
        Map<String, Object> fcrBlock = new LinkedHashMap<>();
        fcrBlock.put(STATUS, UNAVAILABLE);
        fcrBlock.put("value", fcr);
        fcrBlock.put("note", "FCR is not calculated from real feed/weight aggregates yet.");
        out.put("fcr", fcrBlock);
        out.put("eggMetrics", Map.of(STATUS, UNAVAILABLE, "note", "Egg production metrics are not tracked"));
        out.put(STATUS, AVAILABLE);
        return out;
    }

    public Map<String, Object> inventory() {
        Map<String, Object> stats = inventoryService.getDashboardStats();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalValue", stats.get("total_value"));
        out.put("lowStockCount", stats.get("low_stock_count"));
        out.put("expiringCount", stats.get("expiring_count"));
        out.put("totalSkus", stats.get("total_skus"));
        out.put(STATUS, AVAILABLE);
        return out;
    }

    public Map<String, Object> procurement() {
        Map<String, Object> dash = procurementService.dashboard();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("openRequisitions", dash.get("openRequisitions"));
        out.put("openPurchaseOrders", dash.get("openPurchaseOrders"));
        out.put("approvedUnpaidInvoices", dash.get("approvedUnpaidInvoices"));
        out.put("apOutstanding", dash.get("apOutstanding"));
        out.put("invoicesAwaitingMatch", dash.get("invoicesAwaitingMatch"));
        out.put(STATUS, AVAILABLE);
        return out;
    }

    public Map<String, Object> services() {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> consultancy = consultancyService.dashboard();
        Map<String, Object> training = trainingService.dashboard();
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("activeEngagements", consultancy.get("activeEngagements"));
        c.put("openRequests", consultancy.get("openRequests"));
        c.put("revenue", consultancy.get("revenue"));
        c.put("outstandingAmount", consultancy.get("outstandingAmount"));
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("activePrograms", training.get("activePrograms"));
        t.put("upcomingSessionCount", training.get("upcomingSessionCount"));
        t.put("revenue", training.get("revenue"));
        t.put("outstandingAmount", training.get("outstandingAmount"));
        out.put("consultancy", c);
        out.put("training", t);
        out.put(STATUS, AVAILABLE);
        out.put("note", "Service revenue fields come from Finance INCOME by sourceModule.");
        return out;
    }

    public Map<String, Object> workforce() {
        if (!canSeeWorkforce()) {
            return Map.of(STATUS, UNAVAILABLE, "note", "Requires executive:hr or hr:report");
        }
        Map<String, Object> dash = hrService.dashboard();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalEmployees", dash.get("totalEmployees"));
        out.put("activeHeadcount", dash.get("activeHeadcount"));
        out.put("onLeave", dash.get("onLeave"));
        out.put("headcountByDepartment", dash.get("headcountByDepartment"));
        out.put("pendingLeaveRequests", dash.get("pendingLeaveRequests"));
        out.put("currentPayrollPeriod", dash.get("currentPayrollPeriod"));
        out.put("currentPayrollStatus", dash.get("currentPayrollStatus"));
        out.put("currentPayrollGross", dash.get("currentPayrollGross"));
        out.put("currentPayrollNet", dash.get("currentPayrollNet"));
        out.put("salaryPayableOutstanding", dash.get("salaryPayableOutstanding"));
        out.put(STATUS, AVAILABLE);
        out.put("privacyNote", "Aggregates only — no individual salaries or documents.");
        return out;
    }

    public Map<String, Object> alerts(String preset, LocalDate from, LocalDate to) {
        return alerts(ExecutiveDateRange.resolve(preset, from, to));
    }

    Map<String, Object> alerts(ExecutiveDateRange range) {
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate today = LocalDate.now();

        BigDecimal overdueAr = nvl(invoiceRepository.sumOverdueOutstanding(today));
        if (overdueAr.compareTo(ZERO) > 0) {
            list.add(alert("HIGH", "Overdue receivables",
                    "Overdue AR outstanding: " + overdueAr, "/finance/invoices", "FINANCE"));
        }

        try {
            Map<String, Object> inv = inventoryService.getDashboardStats();
            Number low = (Number) inv.get("low_stock_count");
            Number exp = (Number) inv.get("expiring_count");
            if (low != null && low.intValue() > 0) {
                list.add(alert("MEDIUM", "Low stock",
                        low + " SKUs below reorder point", "/inventory", "INVENTORY"));
            }
            if (exp != null && exp.intValue() > 0) {
                list.add(alert("HIGH", "Expiring stock",
                        exp + " batches expiring soon", "/inventory/expiry", "INVENTORY"));
            }
        } catch (Exception ignored) {
            // skip inventory alerts if service unavailable
        }

        if (hasAny(Permissions.PROCUREMENT_READ, Permissions.EXECUTIVE_FINANCE, Permissions.EXECUTIVE_INVENTORY)) {
            try {
                Map<String, Object> proc = procurementService.dashboard();
                Number awaiting = asNumber(proc.get("invoicesAwaitingMatch"));
                Number openReq = asNumber(proc.get("openRequisitions"));
                if (awaiting != null && awaiting.longValue() > 0) {
                    list.add(alert("MEDIUM", "Procurement match pending",
                            awaiting + " invoices awaiting match", "/procurement", "PROCUREMENT"));
                }
                if (openReq != null && openReq.longValue() > 0) {
                    list.add(alert("LOW", "Open requisitions",
                            openReq + " open requisitions", "/procurement", "PROCUREMENT"));
                }
            } catch (Exception ignored) {
                // optional
            }
        }

        Integer todayMortality = dailyFarmRecordRepository.sumMortalityByDate(today);
        int mort = todayMortality != null ? todayMortality : 0;
        if (mort > mortalityAlertThreshold) {
            list.add(alert("HIGH", "Today mortality",
                    "Today mortality count: " + mort, "/farms", "FARM"));
        }

        long pendingRx = prescriptionRepository.countByStatus(PrescriptionStatus.PENDING_PHARMACY)
                + prescriptionRepository.countByStatus(PrescriptionStatus.PENDING)
                + prescriptionRepository.countByStatus(PrescriptionStatus.APPROVED);
        if (pendingRx > 0) {
            list.add(alert("MEDIUM", "Pending prescriptions",
                    pendingRx + " prescriptions awaiting dispense/action",
                    "/veterinary/prescriptions", "VETERINARY"));
        }

        if (hasAny(Permissions.AUDIT_READ, Permissions.EXECUTIVE_DASHBOARD, Permissions.GOVERNANCE_READ)) {
            try {
                Map<String, Object> gov = governanceService.executiveSummary();
                Number pending = asNumber(gov.get("pendingApprovals"));
                Number errors = asNumber(gov.get("controlErrorCount"));
                if (pending != null && pending.longValue() > 0) {
                    list.add(alert("MEDIUM", "Governance: pending approvals",
                            pending + " items awaiting approval (procurement/leave/payroll)",
                            "/governance", "GOVERNANCE"));
                }
                if (errors != null && errors.longValue() > 0) {
                    list.add(alert("HIGH", "Governance control exceptions",
                            errors + " control check(s) in ERROR status",
                            "/governance", "GOVERNANCE"));
                }
            } catch (Exception ignored) {
                // optional governance snippet
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("period", range.toMap());
        out.put("alerts", list);
        out.put("count", list.size());
        out.put(STATUS, AVAILABLE);
        return out;
    }

    public Map<String, Object> definitions() {
        return ExecutiveDefinitions.all();
    }

    // ─── helpers ─────────────────────────────────────────────────

    private List<Map<String, Object>> monthlyTrend(TransactionType type, LocalDate from, LocalDate to, String module) {
        List<Map<String, Object>> rows = new ArrayList<>();
        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            LocalDate mFrom = ym.atDay(1);
            if (mFrom.isBefore(from)) mFrom = from;
            LocalDate mTo = ym.atEndOfMonth();
            if (mTo.isAfter(to)) mTo = to;
            BigDecimal amount = nvl(transactionRepository.sumByTypeDateRangeAndModule(type, mFrom, mTo, module));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("year", ym.getYear());
            row.put("month", ym.getMonthValue());
            row.put("amount", amount);
            rows.add(row);
        }
        return rows;
    }

    private Map<String, BigDecimal> toAmountMap(List<Object[]> rows) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        if (rows == null) return map;
        for (Object[] row : rows) {
            String module = row[0] != null ? row[0].toString() : FinanceSourceTypes.MODULE_MANUAL;
            BigDecimal amount = row[1] instanceof BigDecimal bd ? bd : ZERO;
            map.put(module, amount);
        }
        return map;
    }

    private Map<String, BigDecimal> filterModule(Map<String, BigDecimal> source, String module) {
        Map<String, BigDecimal> filtered = new LinkedHashMap<>();
        if (source.containsKey(module)) {
            filtered.put(module, source.get(module));
        } else {
            filtered.put(module, ZERO);
        }
        return filtered;
    }

    private String normalizeBusinessLine(String businessLine) {
        if (businessLine == null || businessLine.isBlank() || "ALL".equalsIgnoreCase(businessLine.trim())) {
            return null;
        }
        String key = businessLine.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "CHICKEN_SALE", "PHARMACY", "CONSULTANCY", "TRAINING", "CRM", "HR", "PROCUREMENT" -> key;
            case "OTHER" -> FinanceSourceTypes.MODULE_MANUAL;
            default -> key;
        };
    }

    private Map<String, Object> alert(String severity, String title, String message, String link, String module) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("severity", severity);
        a.put("title", title);
        a.put("message", message);
        a.put("link", link);
        a.put("module", module);
        return a;
    }

    private Number asNumber(Object o) {
        return o instanceof Number n ? n : null;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }

    private boolean canSeeFinance() {
        return hasAny(Permissions.EXECUTIVE_FINANCE, Permissions.FINANCE_READ);
    }

    private boolean canSeeSales() {
        return hasAny(Permissions.EXECUTIVE_SALES, Permissions.CRM_READ, Permissions.CRM_REPORT);
    }

    private boolean canSeeFarm() {
        return hasAny(Permissions.EXECUTIVE_FARM, Permissions.FARM_READ);
    }

    private boolean canSeeInventory() {
        return hasAny(Permissions.EXECUTIVE_INVENTORY, Permissions.INVENTORY_READ);
    }

    private boolean canSeeProcurement() {
        return hasAny(Permissions.PROCUREMENT_READ, Permissions.EXECUTIVE_FINANCE, Permissions.EXECUTIVE_INVENTORY);
    }

    private boolean canSeeServices() {
        return hasAny(Permissions.CONSULTANCY_READ, Permissions.TRAINING_READ, Permissions.EXECUTIVE_SALES);
    }

    boolean canSeeWorkforce() {
        return hasAny(Permissions.EXECUTIVE_HR, Permissions.HR_REPORT, Permissions.HR_READ);
    }

    private boolean hasAny(String... authorities) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) return false;
        Set<String> granted = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
        for (String a : authorities) {
            if (granted.contains(a)) return true;
        }
        return false;
    }
}
