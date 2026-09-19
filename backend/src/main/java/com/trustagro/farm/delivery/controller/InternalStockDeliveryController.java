package com.trustagro.farm.delivery.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.farm.delivery.dto.InternalStockDeliveryRequest;
import com.trustagro.farm.delivery.dto.InternalStockDeliveryResponse;
import com.trustagro.farm.delivery.entity.InternalDeliveryStatus;
import com.trustagro.farm.delivery.service.InternalStockDeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal-deliveries")
@RequiredArgsConstructor
public class InternalStockDeliveryController {

    private final InternalStockDeliveryService deliveryService;

    @GetMapping
    @PreAuthorize("hasAuthority('farm:read') or hasAuthority('inventory:read')")
    public ResponseEntity<ApiResponse<List<InternalStockDeliveryResponse>>> list(
            @RequestParam(required = false) InternalDeliveryStatus status) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.list(status)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farm:read') or hasAuthority('inventory:read')")
    public ResponseEntity<ApiResponse<InternalStockDeliveryResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farm:create')")
    public ResponseEntity<ApiResponse<InternalStockDeliveryResponse>> create(
            @Valid @RequestBody InternalStockDeliveryRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Internal delivery created", deliveryService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<InternalStockDeliveryResponse>> update(
            @PathVariable Long id, @Valid @RequestBody InternalStockDeliveryRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Internal delivery updated", deliveryService.update(id, req)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('farm:create') or hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<InternalStockDeliveryResponse>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Submitted", deliveryService.submit(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<InternalStockDeliveryResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Approved", deliveryService.approve(id)));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('inventory:transfer')")
    public ResponseEntity<ApiResponse<InternalStockDeliveryResponse>> issue(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Issued", deliveryService.issue(id)));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<InternalStockDeliveryResponse>> receive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Received", deliveryService.receive(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<InternalStockDeliveryResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Cancelled", deliveryService.cancel(id)));
    }
}
