package com.trustagro.executive;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static KPI definition map for executive consumers and {@code GET /definitions}.
 */
public final class ExecutiveDefinitions {

    private ExecutiveDefinitions() {}

    public static Map<String, Object> all() {
        Map<String, Object> defs = new LinkedHashMap<>();
        defs.put("totalRevenue", kpi(
                "Period revenue from Finance P&L / INCOME postings",
                "Finance",
                "Sum of INCOME ledger activity in range (not TRANSFER payments)",
                "transactionDate inclusive",
                "TRANSFER/cash receipts, unpaid AR increases"));
        defs.put("totalExpenses", kpi(
                "Period expenses from Finance P&L",
                "Finance",
                "Sum of EXPENSE ledger activity in range",
                "transactionDate inclusive",
                "Missing COGS not treated as zero"));
        defs.put("netProfit", kpi(
                "Revenue minus expenses for the period",
                "Finance",
                "totalRevenue - totalExpenses from P&L",
                "transactionDate inclusive",
                "Incomplete cost allocations → treat profitability carefully"));
        defs.put("cashInHand", kpi(
                "Point-in-time cash account balance",
                "Finance CoA 1000",
                "ChartOfAccount.balance for cash",
                "as-of now",
                "Not period revenue"));
        defs.put("bankBalance", kpi(
                "Point-in-time bank account balance",
                "Finance CoA 1010",
                "ChartOfAccount.balance for bank",
                "as-of now",
                "Not period revenue"));
        defs.put("accountsReceivable", kpi(
                "Open invoice outstanding",
                "Finance invoices",
                "SUM(total-paid) where status SENT/PARTIALLY_PAID/OVERDUE",
                "as-of now",
                "Not revenue; cash receipts reduce AR via TRANSFER"));
        defs.put("accountsPayable", kpi(
                "Accounts payable ledger balance",
                "Finance CoA 2000 + Procurement",
                "CoA balance; aging from procurement when authorized",
                "as-of now",
                "Aging unavailable without procurement:read / executive:finance"));
        defs.put("revenueByModule", kpi(
                "INCOME sums grouped by sourceModule",
                "Finance",
                "GROUP BY sourceModule for INCOME only",
                "transactionDate inclusive",
                "Never counts TRANSFER payments as revenue"));
        defs.put("fcr", kpi(
                "Feed conversion ratio",
                "Farm",
                "Not calculated — FlockManagementService.calculateFCR returns null",
                "n/a",
                "Always UNAVAILABLE until real feed/weight aggregates exist"));
        defs.put("workforce", kpi(
                "Aggregated HR headcount and payroll totals",
                "HR",
                "HrService.dashboard aggregates only",
                "as-of now / current payroll period",
                "No individual salaries or documents; requires executive:hr or hr:report"));
        defs.put("conversionRate", kpi(
                "CRM lead conversion percentage",
                "CRM",
                "convertedLeads / totalLeads * 100",
                "lifetime leads (commercial dashboard)",
                "Never invented"));
        return defs;
    }

    private static Map<String, String> kpi(String description, String sourceModule,
                                           String calculation, String dateBasis, String exclusions) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("description", description);
        m.put("sourceModule", sourceModule);
        m.put("calculation", calculation);
        m.put("dateBasis", dateBasis);
        m.put("exclusions", exclusions);
        m.put("authority", "See executive:* / module:read gates");
        return m;
    }
}
