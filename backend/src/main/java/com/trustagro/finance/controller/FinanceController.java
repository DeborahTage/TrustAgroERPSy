package com.trustagro.finance.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.entity.BusinessUnit;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.finance.service.InvoiceService;
import com.trustagro.finance.service.ReconciliationService;
import com.trustagro.finance.dto.TransactionRequest;
import com.trustagro.finance.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                financeService.getAllTransactions().stream()
                        .map(this::toResponse)
                        .toList()
        ));
    }

    @PostMapping("/transactions")
    @PreAuthorize("hasAuthority('finance:create')")
    public ResponseEntity<ApiResponse<TransactionResponse>> postTransaction(@Valid @RequestBody TransactionRequest req) {
        FinanceTransaction tx = financeService.postTransaction(req.getTransactionType(), req.getAmount(),
                BusinessUnit.GENERAL,
                req.getDebitAccountCode(), req.getCreditAccountCode(), req.getDescription(),
                "MANUAL", req.getReferenceType(), null);
        TransactionResponse response = toResponse(tx);
        if (response != null) {
            if (response.getDebitAccountCode() == null && req.getDebitAccountCode() != null) {
                response.setDebitAccountCode(req.getDebitAccountCode().trim());
            }
            if (response.getCreditAccountCode() == null && req.getCreditAccountCode() != null) {
                response.setCreditAccountCode(req.getCreditAccountCode().trim());
            }
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private TransactionResponse toResponse(FinanceTransaction tx) {
        if (tx == null) return null;
        TransactionResponse response = new TransactionResponse();
        response.setId(tx.getId());
        response.setTransactionType(tx.getTransactionType());
        response.setCategory(tx.getCategory());
        response.setAmount(tx.getAmount());
        response.setDebitAccountCode(tx.getDebitAccount() != null ? tx.getDebitAccount().getAccountCode() : null);
        response.setCreditAccountCode(tx.getCreditAccount() != null ? tx.getCreditAccount().getAccountCode() : null);
        response.setSourceModule(tx.getSourceModule());
        response.setReferenceType(tx.getReferenceType());
        response.setReferenceId(tx.getReferenceId());
        response.setBusinessUnit(tx.getBusinessUnit());
        response.setDescription(tx.getDescription());
        response.setTransactionDate(tx.getTransactionDate());
        response.setCreatedAt(tx.getCreatedAt());
        return response;
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
