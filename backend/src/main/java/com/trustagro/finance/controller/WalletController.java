package com.trustagro.finance.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.finance.entity.CustomerWallet;
import com.trustagro.finance.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<List<CustomerWallet>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(walletService.getAllWallets()));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('finance:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(walletService.getWalletSummary()));
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasAuthority('finance:create')")
    public ResponseEntity<ApiResponse<CustomerWallet>> deposit(@RequestBody Map<String, Object> req) {
        CustomerWallet wallet = walletService.deposit(
                Long.valueOf(req.get("clientId").toString()),
                req.get("clientName").toString(),
                new BigDecimal(req.get("amount").toString())
        );
        return ResponseEntity.ok(ApiResponse.success("Deposit successful", wallet));
    }
}
