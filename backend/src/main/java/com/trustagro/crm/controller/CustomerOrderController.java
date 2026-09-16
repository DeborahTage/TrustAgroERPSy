package com.trustagro.crm.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.crm.dto.OrderPaymentRequest;
import com.trustagro.crm.dto.OrderPaymentResponse;
import com.trustagro.crm.dto.OrderRequest;
import com.trustagro.crm.dto.OrderResponse;
import com.trustagro.crm.entity.CustomerOrder;
import com.trustagro.crm.service.CustomerOrderService;
import jakarta.validation.Valid;
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
    @PreAuthorize("hasAnyAuthority('crm:read','crm:order')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) Long clientId) {
        if (clientId != null) {
            return ResponseEntity.ok(ApiResponse.success(orderService.getOrdersByClient(clientId)));
        }
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('crm:read','crm:order')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrder(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('crm:create','crm:order')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Order created", orderService.createOrder(req)));
    }

    /** Legacy entity body create — kept for backward compatibility. */
    @PostMapping("/legacy")
    @PreAuthorize("hasAnyAuthority('crm:create','crm:order')")
    public ResponseEntity<ApiResponse<CustomerOrder>> createOrderLegacy(@RequestBody CustomerOrder order) {
        return ResponseEntity.ok(ApiResponse.success("Order created", orderService.createOrder(order)));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('crm:order_confirm')")
    public ResponseEntity<ApiResponse<OrderResponse>> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Order confirmed", orderService.confirm(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('crm:order')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", orderService.cancel(id)));
    }

    @PostMapping("/{id}/fulfill")
    @PreAuthorize("hasAuthority('crm:order')")
    public ResponseEntity<ApiResponse<OrderResponse>> fulfill(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean partial) {
        return ResponseEntity.ok(ApiResponse.success(orderService.fulfill(id, partial)));
    }

    @PostMapping("/{id}/invoice")
    @PreAuthorize("hasAuthority('crm:order_invoice')")
    public ResponseEntity<ApiResponse<OrderResponse>> createInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Invoice created", orderService.createInvoice(id)));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('crm:order_pay')")
    public ResponseEntity<ApiResponse<OrderResponse>> recordPayment(
            @PathVariable Long id, @Valid @RequestBody OrderPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Payment recorded", orderService.recordPayment(id, req)));
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAnyAuthority('crm:read','crm:order_pay')")
    public ResponseEntity<ApiResponse<List<OrderPaymentResponse>>> listPayments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.listPayments(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('crm:update','crm:order')")
    public ResponseEntity<ApiResponse<CustomerOrder>> updateStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", orderService.updateStatus(id, status)));
    }
}
