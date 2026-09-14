package com.trustagro.finance.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.finance.service.FlockFinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/finance/flocks")
@RequiredArgsConstructor
public class FlockFinancialController {

    private final FlockFinancialService flockFinancialService;

    @GetMapping("/{flockId}/profit-loss")
    @PreAuthorize("hasAnyAuthority('finance:read','farm:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFlockProfitLoss(@PathVariable Long flockId) {
        return ResponseEntity.ok(ApiResponse.success(flockFinancialService.getFlockProfitLoss(flockId)));
    }
}
