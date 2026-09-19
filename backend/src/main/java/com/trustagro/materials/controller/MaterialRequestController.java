package com.trustagro.materials.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.materials.dto.*;
import com.trustagro.materials.entity.MaterialRequestStatus;
import com.trustagro.materials.entity.MaterialRequestType;
import com.trustagro.materials.service.MaterialDocumentService;
import com.trustagro.materials.service.MaterialRequestReportService;
import com.trustagro.materials.service.MaterialRequestService;
import com.trustagro.security.Permissions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/material-requests")
@RequiredArgsConstructor
public class MaterialRequestController {

    private final MaterialRequestService requestService;
    private final MaterialRequestReportService reportService;
    private final MaterialDocumentService documentService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_READ + "')")
    public ResponseEntity<ApiResponse<List<MaterialRequestResponse>>> list(
            @RequestParam(required = false) MaterialRequestStatus status,
            @RequestParam(required = false) MaterialRequestType type,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) String queue) {
        return ResponseEntity.ok(ApiResponse.success(
                requestService.list(status, type, farmId, requesterId, queue)));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_READ + "')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(requestService.dashboard()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_READ + "')")
    public ResponseEntity<ApiResponse<MaterialRequestResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(requestService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_CREATE + "')")
    public ResponseEntity<ApiResponse<MaterialRequestResponse>> create(
            @Valid @RequestBody MaterialRequestCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Material request created", requestService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_UPDATE + "')")
    public ResponseEntity<ApiResponse<MaterialRequestResponse>> update(
            @PathVariable Long id, @Valid @RequestBody MaterialRequestCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Material request updated", requestService.update(id, req)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_SUBMIT + "')")
    public ResponseEntity<ApiResponse<MaterialRequestResponse>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Submitted", requestService.submit(id)));
    }

    @PostMapping("/{id}/finance-approve")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_APPROVE + "') or hasAuthority('" + Permissions.FINANCE_APPROVE + "')")
    public ResponseEntity<ApiResponse<MaterialRequestResponse>> financeApprove(
            @PathVariable Long id, @RequestBody(required = false) FinanceDecisionRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Approved", requestService.financeApprove(id, body)));
    }

    @PostMapping("/{id}/finance-reject")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_APPROVE + "') or hasAuthority('" + Permissions.FINANCE_APPROVE + "')")
    public ResponseEntity<ApiResponse<MaterialRequestResponse>> financeReject(
            @PathVariable Long id, @RequestBody FinanceDecisionRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Rejected", requestService.financeReject(id, body)));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_APPROVE + "') or hasAuthority('" + Permissions.FINANCE_APPROVE + "')")
    public ResponseEntity<ApiResponse<MaterialRequestResponse>> returnForCorrection(
            @PathVariable Long id, @RequestBody FinanceDecisionRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Returned", requestService.returnForCorrection(id, body)));
    }

    @PostMapping("/{id}/authorize-purchase")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_UPDATE + "') or hasAuthority('" + Permissions.PROCUREMENT_CREATE + "')")
    public ResponseEntity<ApiResponse<MaterialRequestResponse>> authorizePurchase(
            @PathVariable Long id, @RequestBody(required = false) AuthorizePurchaseRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Purchase authorized",
                requestService.authorizePurchase(id, body)));
    }

    @PostMapping("/{id}/disburse")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_APPROVE + "') or hasAuthority('" + Permissions.FINANCE_CREATE + "') or hasAuthority('" + Permissions.PROCUREMENT_PAYMENT + "')")
    public ResponseEntity<ApiResponse<MaterialRequestResponse>> disburse(
            @PathVariable Long id, @Valid @RequestBody DisbursementRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Disbursement recorded",
                requestService.recordDisbursement(id, body)));
    }

    @PostMapping("/{id}/goods-receipt")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_RECEIVE + "') or hasAuthority('" + Permissions.MATERIAL_REQUEST_FULFILL + "')")
    public ResponseEntity<ApiResponse<MaterialRequestResponse>> goodsReceipt(
            @PathVariable Long id, @Valid @RequestBody GoodsReceiptRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Goods received",
                requestService.registerGoodsReceipt(id, body)));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_FULFILL + "')")
    public ResponseEntity<ApiResponse<MaterialRequestResponse>> issue(
            @PathVariable Long id, @Valid @RequestBody IssueRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Issued", requestService.issueInternal(id, body)));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_READ + "') or hasAuthority('" + Permissions.FARM_UPDATE + "')")
    public ResponseEntity<ApiResponse<MaterialRequestResponse>> acknowledge(
            @PathVariable Long id, @RequestBody(required = false) AcknowledgeRequest body) {
        return ResponseEntity.ok(ApiResponse.success("Acknowledged",
                requestService.acknowledgeReceipt(id, body)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_UPDATE + "')")
    public ResponseEntity<ApiResponse<MaterialRequestResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Cancelled", requestService.cancel(id)));
    }

    @GetMapping("/reports/summary")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_REPORT + "')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> reportSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) MaterialRequestType type,
            @RequestParam(required = false) MaterialRequestStatus status,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.summary(from, to, type, status, farmId, department)));
    }

    @GetMapping("/reports/by-status")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_REPORT + "')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportByStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) MaterialRequestType type,
            @RequestParam(required = false) Long farmId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.byStatus(from, to, type, farmId)));
    }

    @GetMapping("/reports/by-department")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_REPORT + "')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportByDepartment(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) MaterialRequestType type,
            @RequestParam(required = false) Long farmId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.byDepartment(from, to, type, farmId)));
    }

    @GetMapping("/reports/fulfillment")
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_REPORT + "')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reportFulfillment(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long farmId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.fulfillment(from, to, farmId)));
    }

    @GetMapping(value = "/documents/{type}/{id}", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAuthority('" + Permissions.MATERIAL_REQUEST_READ + "')")
    public ResponseEntity<String> document(@PathVariable String type, @PathVariable Long id) {
        return ResponseEntity.ok(documentService.render(type, id));
    }
}
