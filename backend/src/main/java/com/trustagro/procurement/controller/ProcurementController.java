package com.trustagro.procurement.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.inventory.entity.PurchaseOrderStatus;
import com.trustagro.procurement.dto.*;
import com.trustagro.procurement.entity.GoodsReceiptStatus;
import com.trustagro.procurement.entity.ProcRequisitionStatus;
import com.trustagro.procurement.entity.SupplierInvoiceStatus;
import com.trustagro.procurement.service.ProcurementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('procurement:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(procurementService.dashboard()));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyAuthority('procurement:report','procurement:read','reports:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reports() {
        return ResponseEntity.ok(ApiResponse.success(procurementService.reports()));
    }

    @GetMapping("/ap/aging")
    @PreAuthorize("hasAnyAuthority('procurement:report','procurement:invoice','procurement:payment','procurement:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> apAging() {
        return ResponseEntity.ok(ApiResponse.success(procurementService.apAging()));
    }

    @GetMapping("/suppliers/{supplierId}/statement")
    @PreAuthorize("hasAnyAuthority('procurement:report','procurement:invoice','procurement:payment','procurement:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> supplierStatement(@PathVariable Long supplierId) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.supplierStatement(supplierId)));
    }

    // ─── Requisitions ────────────────────────────────────────────

    @GetMapping("/requisitions")
    @PreAuthorize("hasAuthority('procurement:read')")
    public ResponseEntity<ApiResponse<List<RequisitionResponse>>> listRequisitions(
            @RequestParam(required = false) ProcRequisitionStatus status) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.listRequisitions(status)));
    }

    @GetMapping("/requisitions/{id}")
    @PreAuthorize("hasAuthority('procurement:read')")
    public ResponseEntity<ApiResponse<RequisitionResponse>> getRequisition(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.getRequisition(id)));
    }

    @PostMapping("/requisitions")
    @PreAuthorize("hasAuthority('procurement:create')")
    public ResponseEntity<ApiResponse<RequisitionResponse>> createRequisition(
            @Valid @RequestBody RequisitionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.createRequisition(req)));
    }

    @PutMapping("/requisitions/{id}")
    @PreAuthorize("hasAuthority('procurement:update')")
    public ResponseEntity<ApiResponse<RequisitionResponse>> updateRequisition(
            @PathVariable Long id, @Valid @RequestBody RequisitionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.updateRequisition(id, req)));
    }

    @PostMapping("/requisitions/{id}/submit")
    @PreAuthorize("hasAnyAuthority('procurement:create','procurement:update')")
    public ResponseEntity<ApiResponse<RequisitionResponse>> submitRequisition(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.submitRequisition(id)));
    }

    @PostMapping("/requisitions/{id}/review")
    @PreAuthorize("hasAuthority('procurement:approve')")
    public ResponseEntity<ApiResponse<RequisitionResponse>> reviewRequisition(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.reviewRequisition(id)));
    }

    @PostMapping("/requisitions/{id}/approve")
    @PreAuthorize("hasAuthority('procurement:approve')")
    public ResponseEntity<ApiResponse<RequisitionResponse>> approveRequisition(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.approveRequisition(id)));
    }

    @PostMapping("/requisitions/{id}/reject")
    @PreAuthorize("hasAuthority('procurement:approve')")
    public ResponseEntity<ApiResponse<RequisitionResponse>> rejectRequisition(
            @PathVariable Long id, @RequestBody(required = false) DecisionRequest body) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.rejectRequisition(id, body)));
    }

    @PostMapping("/requisitions/{id}/cancel")
    @PreAuthorize("hasAuthority('procurement:cancel')")
    public ResponseEntity<ApiResponse<RequisitionResponse>> cancelRequisition(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.cancelRequisition(id)));
    }

    @PostMapping("/requisitions/{id}/convert-po")
    @PreAuthorize("hasAnyAuthority('procurement:create','procurement:update')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> convertRequisitionToPo(
            @PathVariable Long id, @RequestParam Long supplierId) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.convertRequisitionToPo(id, supplierId)));
    }

    // ─── Purchase Orders ─────────────────────────────────────────

    @GetMapping("/purchase-orders")
    @PreAuthorize("hasAuthority('procurement:read')")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> listPurchaseOrders(
            @RequestParam(required = false) PurchaseOrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.listPurchaseOrders(status)));
    }

    @GetMapping("/purchase-orders/{id}")
    @PreAuthorize("hasAuthority('procurement:read')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getPurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.getPurchaseOrder(id)));
    }

    @PostMapping("/purchase-orders")
    @PreAuthorize("hasAuthority('procurement:create')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequest req) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.createPurchaseOrder(req)));
    }

    @PutMapping("/purchase-orders/{id}")
    @PreAuthorize("hasAuthority('procurement:update')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updatePurchaseOrder(
            @PathVariable Long id, @Valid @RequestBody PurchaseOrderRequest req) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.updatePurchaseOrder(id, req)));
    }

    @PostMapping("/purchase-orders/{id}/submit")
    @PreAuthorize("hasAnyAuthority('procurement:create','procurement:update')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> submitPurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.submitPurchaseOrder(id)));
    }

    @PostMapping("/purchase-orders/{id}/approve")
    @PreAuthorize("hasAuthority('procurement:approve')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> approvePurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.approvePurchaseOrder(id)));
    }

    @PostMapping("/purchase-orders/{id}/send")
    @PreAuthorize("hasAnyAuthority('procurement:update','procurement:create')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> sendPurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.sendPurchaseOrder(id)));
    }

    @PostMapping("/purchase-orders/{id}/cancel")
    @PreAuthorize("hasAuthority('procurement:cancel')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> cancelPurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.cancelPurchaseOrder(id)));
    }

    @PostMapping("/purchase-orders/{id}/close")
    @PreAuthorize("hasAnyAuthority('procurement:update','procurement:approve')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> closePurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.closePurchaseOrder(id)));
    }

    // ─── Goods Receipts ──────────────────────────────────────────

    @GetMapping("/goods-receipts")
    @PreAuthorize("hasAuthority('procurement:read')")
    public ResponseEntity<ApiResponse<List<GoodsReceiptResponse>>> listGoodsReceipts(
            @RequestParam(required = false) GoodsReceiptStatus status) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.listGoodsReceipts(status)));
    }

    @GetMapping("/goods-receipts/{id}")
    @PreAuthorize("hasAuthority('procurement:read')")
    public ResponseEntity<ApiResponse<GoodsReceiptResponse>> getGoodsReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.getGoodsReceipt(id)));
    }

    @PostMapping("/goods-receipts")
    @PreAuthorize("hasAuthority('procurement:receive')")
    public ResponseEntity<ApiResponse<GoodsReceiptResponse>> createGoodsReceipt(
            @Valid @RequestBody GoodsReceiptRequest req) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.createGoodsReceipt(req)));
    }

    @PostMapping("/goods-receipts/{id}/post")
    @PreAuthorize("hasAuthority('procurement:receive')")
    public ResponseEntity<ApiResponse<GoodsReceiptResponse>> postGoodsReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.postGoodsReceipt(id)));
    }

    @PostMapping("/goods-receipts/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('procurement:cancel','procurement:receive')")
    public ResponseEntity<ApiResponse<GoodsReceiptResponse>> cancelGoodsReceipt(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.cancelGoodsReceipt(id)));
    }

    // ─── Supplier Invoices ───────────────────────────────────────

    @GetMapping("/supplier-invoices")
    @PreAuthorize("hasAnyAuthority('procurement:read','procurement:invoice')")
    public ResponseEntity<ApiResponse<List<SupplierInvoiceResponse>>> listSupplierInvoices(
            @RequestParam(required = false) SupplierInvoiceStatus status) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.listSupplierInvoices(status)));
    }

    @GetMapping("/supplier-invoices/{id}")
    @PreAuthorize("hasAnyAuthority('procurement:read','procurement:invoice')")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponse>> getSupplierInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.getSupplierInvoice(id)));
    }

    @PostMapping("/supplier-invoices")
    @PreAuthorize("hasAuthority('procurement:invoice')")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponse>> createSupplierInvoice(
            @Valid @RequestBody SupplierInvoiceRequest req) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.createSupplierInvoice(req)));
    }

    @PostMapping("/supplier-invoices/{id}/submit")
    @PreAuthorize("hasAuthority('procurement:invoice')")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponse>> submitSupplierInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.submitSupplierInvoice(id)));
    }

    @PostMapping("/supplier-invoices/{id}/match")
    @PreAuthorize("hasAuthority('procurement:invoice')")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponse>> matchSupplierInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.matchSupplierInvoice(id)));
    }

    @PostMapping("/supplier-invoices/{id}/approve")
    @PreAuthorize("hasAuthority('procurement:invoice')")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponse>> approveSupplierInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.approveSupplierInvoice(id)));
    }

    @PostMapping("/supplier-invoices/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('procurement:invoice','procurement:cancel')")
    public ResponseEntity<ApiResponse<SupplierInvoiceResponse>> cancelSupplierInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.cancelSupplierInvoice(id)));
    }

    // ─── Payments ────────────────────────────────────────────────

    @GetMapping("/supplier-payments")
    @PreAuthorize("hasAnyAuthority('procurement:read','procurement:payment')")
    public ResponseEntity<ApiResponse<List<SupplierPaymentResponse>>> listPayments() {
        return ResponseEntity.ok(ApiResponse.success(procurementService.listPayments()));
    }

    @GetMapping("/supplier-payments/{id}")
    @PreAuthorize("hasAnyAuthority('procurement:read','procurement:payment')")
    public ResponseEntity<ApiResponse<SupplierPaymentResponse>> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.getPayment(id)));
    }

    @PostMapping("/supplier-payments")
    @PreAuthorize("hasAuthority('procurement:payment')")
    public ResponseEntity<ApiResponse<SupplierPaymentResponse>> createPayment(
            @Valid @RequestBody SupplierPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(procurementService.createPayment(req)));
    }
}
