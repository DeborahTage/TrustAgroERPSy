package com.trustagro.crm.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.crm.entity.CustomerOrder;
import com.trustagro.crm.service.CustomerOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crm/orders")
@RequiredArgsConstructor
public class CustomerOrderController {

    private final CustomerOrderService orderService;

    @GetMapping
    @PreAuthorize("hasAuthority('crm:read')")
    public ResponseEntity<ApiResponse<List<CustomerOrder>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('crm:create')")
    public ResponseEntity<ApiResponse<CustomerOrder>> createOrder(@RequestBody CustomerOrder order) {
        return ResponseEntity.ok(ApiResponse.success("Order created", orderService.createOrder(order)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('crm:update')")
    public ResponseEntity<ApiResponse<CustomerOrder>> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", orderService.updateStatus(id, status)));
    }
}
