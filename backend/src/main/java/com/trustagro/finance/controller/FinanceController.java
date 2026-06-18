package com.trustagro.finance.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.finance.service.ReconciliationService;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.entity.BusinessUnit;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;
    private final ReconciliationService reconciliationService;
    private final com.trustagro.finance.repository.FinanceAuditLogRepository auditLogRepository;


    @GetMapping("/audit-trail")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<List<com.trustagro.finance.entity.FinanceAuditLog>>> getAuditTrail() {
        return ResponseEntity.ok(ApiResponse.success(auditLogRepository.findTop100ByOrderByPostedAtDesc()));
    }


    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','GENERAL_MANAGER','FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(financeService.getDashboardSummary()));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','GENERAL_MANAGER','FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<List<FinanceTransaction>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(financeService.getAllTransactions()));
    }

    @PostMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<FinanceTransaction>> postTransaction(@RequestBody Map<String, Object> req) {
        TransactionType type = TransactionType.valueOf(req.getOrDefault("transactionType", "INCOME").toString());
        BigDecimal amount = new BigDecimal(req.get("amount").toString());
        String debit = (String) req.get("debitAccountCode");
        String credit = (String) req.get("creditAccountCode");
        String description = (String) req.getOrDefault("description", "");
        String refType = (String) req.getOrDefault("referenceType", "MANUAL");
        FinanceTransaction tx = financeService.postTransaction(type, amount, BusinessUnit.GENERAL,
                debit, credit, description, "MANUAL", refType, null);
        return ResponseEntity.ok(ApiResponse.success(tx));
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<List<com.trustagro.finance.entity.ChartOfAccount>>> getAccounts() {
        return ResponseEntity.ok(ApiResponse.success(financeService.getAllAccounts()));
    }

    @PostMapping("/reconciliation")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Object>> reconcile(@RequestBody Map<String, Object> req) {
        BigDecimal system = new BigDecimal(req.get("systemAmount").toString());
        BigDecimal actual = new BigDecimal(req.get("actualAmount").toString());
        String notes = (String) req.get("notes");
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.performReconciliation(system, actual, notes)));
    }

    @GetMapping("/reconciliations")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<List<com.trustagro.finance.entity.CashReconciliation>>> getReconciliations() {
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.getAllReconciliations()));
    }

    @PostMapping("/invoices/pos")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE_OFFICER','PHARMACIST')")
    public ResponseEntity<ApiResponse<Object>> createPosInvoice(@RequestBody Map<String, Object> req) {
        BigDecimal amount = new BigDecimal(req.get("amount").toString());
        String customer = (String) req.get("customerName");
        financeService.postRevenue(amount, null, "4100", 
                "POS Invoice: " + customer, "POS", "POS_INVOICE", null);
        return ResponseEntity.ok(ApiResponse.success("POS Invoice created and logged"));
    }
}


