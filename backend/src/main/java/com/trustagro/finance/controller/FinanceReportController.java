package com.trustagro.finance.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.finance.service.FinanceReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/reports")
@RequiredArgsConstructor
public class FinanceReportController {

    private final FinanceReportService reportService;

    @GetMapping("/profit-loss")
    @PreAuthorize("hasAnyRole('ADMIN','GENERAL_MANAGER','FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfitLoss(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getProfitAndLoss(from, to)));
    }

    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAnyRole('ADMIN','GENERAL_MANAGER','FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalanceSheet() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getBalanceSheet()));
    }
}
