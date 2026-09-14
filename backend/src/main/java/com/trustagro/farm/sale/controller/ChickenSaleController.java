package com.trustagro.farm.sale.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.farm.sale.dto.ChickenSalePaymentRequest;
import com.trustagro.farm.sale.dto.ChickenSaleRequest;
import com.trustagro.farm.sale.dto.ChickenSaleResponse;
import com.trustagro.farm.sale.entity.ChickenSaleStatus;
import com.trustagro.farm.sale.service.ChickenSaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chicken-sales")
@RequiredArgsConstructor
public class ChickenSaleController {

    private final ChickenSaleService chickenSaleService;

    @GetMapping
    @PreAuthorize("hasAuthority('chickensale:read')")
    public ResponseEntity<ApiResponse<List<ChickenSaleResponse>>> list(
            @RequestParam(required = false) ChickenSaleStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long flockId,
            @RequestParam(required = false) Long farmId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                chickenSaleService.list(status, customerId, flockId, farmId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('chickensale:read')")
    public ResponseEntity<ApiResponse<ChickenSaleResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(chickenSaleService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('chickensale:create')")
    public ResponseEntity<ApiResponse<ChickenSaleResponse>> create(@Valid @RequestBody ChickenSaleRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Chicken sale created", chickenSaleService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('chickensale:update')")
    public ResponseEntity<ApiResponse<ChickenSaleResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ChickenSaleRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Chicken sale updated", chickenSaleService.update(id, req)));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('chickensale:confirm')")
    public ResponseEntity<ApiResponse<ChickenSaleResponse>> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Chicken sale confirmed", chickenSaleService.confirm(id)));
    }

    @PostMapping("/{id}/invoice")
    @PreAuthorize("hasAuthority('chickensale:invoice')")
    public ResponseEntity<ApiResponse<ChickenSaleResponse>> invoice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Invoice created", chickenSaleService.invoice(id)));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('chickensale:pay')")
    public ResponseEntity<ApiResponse<ChickenSaleResponse>> pay(
            @PathVariable Long id, @Valid @RequestBody ChickenSalePaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Payment recorded", chickenSaleService.recordPayment(id, req)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('chickensale:confirm')")
    public ResponseEntity<ApiResponse<ChickenSaleResponse>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Chicken sale completed", chickenSaleService.complete(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('chickensale:cancel')")
    public ResponseEntity<ApiResponse<ChickenSaleResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Chicken sale cancelled", chickenSaleService.cancel(id)));
    }
}
