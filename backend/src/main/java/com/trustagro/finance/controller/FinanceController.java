package com.trustagro.finance.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.finance.service.InvoiceService;
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
    private final InvoiceService invoiceService;


    @GetMapping("/audit-trail")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<List<com.trustagro.finance.entity.FinanceAuditLog>>> getAuditTrail() {
        return ResponseEntity.ok(ApiResponse.success(auditLogRepository.findTop100ByOrderByPostedAtDesc()));
    }


    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(financeService.getDashboardSummary()));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<List<FinanceTransaction>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(financeService.getAllTransactions()));
    }

    @PostMapping("/transactions")
    @PreAuthorize("hasAuthority('finance:create')")
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
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<List<com.trustagro.finance.entity.ChartOfAccount>>> getAccounts() {
        return ResponseEntity.ok(ApiResponse.success(financeService.getAllAccounts()));
    }

    @PostMapping("/reconciliation")
    @PreAuthorize("hasAuthority('finance:reconcile')")
    public ResponseEntity<ApiResponse<Object>> reconcile(@RequestBody Map<String, Object> req) {
        BigDecimal system = new BigDecimal(req.get("systemAmount").toString());
        BigDecimal actual = new BigDecimal(req.get("actualAmount").toString());
        String notes = (String) req.get("notes");
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.performReconciliation(system, actual, notes)));
    }

    @GetMapping("/reconciliations")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<List<com.trustagro.finance.entity.CashReconciliation>>> getReconciliations() {
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.getAllReconciliations()));
    }

    @GetMapping("/cash/expected")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExpectedCash() {
        return ResponseEntity.ok(ApiResponse.success(reconciliationService.getExpectedCashSnapshot()));
    }

    @GetMapping("/receivables")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<Object>> getReceivables() {
        return ResponseEntity.ok(ApiResponse.success(invoiceService.getOpenReceivables()));
    }
}
