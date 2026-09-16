package com.trustagro.crm.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.crm.dto.*;
import com.trustagro.crm.entity.ClientStatus;
import com.trustagro.crm.entity.CrmLeadStatus;
import com.trustagro.crm.entity.CrmTaskStatus;
import com.trustagro.crm.entity.OpportunityStage;
import com.trustagro.crm.entity.QuotationStatus;
import com.trustagro.crm.service.CrmCommercialService;
import com.trustagro.crm.service.CrmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/crm")
@RequiredArgsConstructor
public class CrmController {

    private final CrmService crmService;
    private final CrmCommercialService commercialService;

    // ─── Clients ─────────────────────────────────────────────────

    @GetMapping("/clients")
    @PreAuthorize("hasAuthority('crm:read')")
    public ResponseEntity<ApiResponse<List<CrmClientResponse>>> getClients(
            @RequestParam(required = false) ClientStatus status) {
        if (status != null) {
            return ResponseEntity.ok(ApiResponse.success(crmService.getClientsByStatus(status)));
        }
        return ResponseEntity.ok(ApiResponse.success(crmService.getAllClients()));
    }

    @GetMapping("/clients/{id}")
    @PreAuthorize("hasAuthority('crm:read')")
    public ResponseEntity<ApiResponse<CrmClientResponse>> getClient(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(crmService.getClientById(id)));
    }

    @GetMapping("/clients/{id}/360")
    @PreAuthorize("hasAuthority('crm:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> customer360(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.customer360(id)));
    }

    @PostMapping("/clients")
    @PreAuthorize("hasAuthority('crm:create')")
    public ResponseEntity<ApiResponse<CrmClientResponse>> createClient(@Valid @RequestBody CrmClientRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Client created", crmService.createClient(req)));
    }

    @PutMapping("/clients/{id}")
    @PreAuthorize("hasAuthority('crm:update')")
    public ResponseEntity<ApiResponse<CrmClientResponse>> updateClient(
            @PathVariable Long id, @Valid @RequestBody CrmClientRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Client updated", crmService.updateClient(id, req)));
    }

    /** Legacy client-status LEAD list (not CrmLead pipeline). */
    @GetMapping("/client-leads")
    @PreAuthorize("hasAnyAuthority('crm:read','marketing:read')")
    public ResponseEntity<ApiResponse<List<CrmClientResponse>>> getClientLeads() {
        return ResponseEntity.ok(ApiResponse.success(crmService.getClientLeads()));
    }

    // ─── Pipeline leads (CrmLead) ────────────────────────────────

    @GetMapping("/leads")
    @PreAuthorize("hasAnyAuthority('crm:read','crm:lead')")
    public ResponseEntity<ApiResponse<List<LeadResponse>>> getLeads(
            @RequestParam(required = false) CrmLeadStatus status) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.listLeads(status)));
    }

    @GetMapping("/leads/{id}")
    @PreAuthorize("hasAnyAuthority('crm:read','crm:lead')")
    public ResponseEntity<ApiResponse<LeadResponse>> getLead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.getLead(id)));
    }

    @PostMapping("/leads")
    @PreAuthorize("hasAuthority('crm:lead')")
    public ResponseEntity<ApiResponse<LeadResponse>> createLead(@Valid @RequestBody LeadRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Lead created", commercialService.createLead(req)));
    }

    @PutMapping("/leads/{id}")
    @PreAuthorize("hasAuthority('crm:lead')")
    public ResponseEntity<ApiResponse<LeadResponse>> updateLead(
            @PathVariable Long id, @Valid @RequestBody LeadRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Lead updated", commercialService.updateLead(id, req)));
    }

    @PatchMapping("/leads/{id}/status")
    @PreAuthorize("hasAuthority('crm:lead')")
    public ResponseEntity<ApiResponse<LeadResponse>> changeLeadStatus(
            @PathVariable Long id, @Valid @RequestBody LeadStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.changeLeadStatus(id, req)));
    }

    @PostMapping("/leads/{id}/convert")
    @PreAuthorize("hasAuthority('crm:lead_convert')")
    public ResponseEntity<ApiResponse<LeadResponse>> convertLead(
            @PathVariable Long id, @RequestBody(required = false) LeadConvertRequest req) {
        if (req == null) req = new LeadConvertRequest();
        return ResponseEntity.ok(ApiResponse.success("Lead converted", commercialService.convertLead(id, req)));
    }

    // ─── Opportunities ───────────────────────────────────────────

    @GetMapping("/opportunities")
    @PreAuthorize("hasAnyAuthority('crm:read','crm:opportunity')")
    public ResponseEntity<ApiResponse<List<OpportunityResponse>>> listOpportunities(
            @RequestParam(required = false) OpportunityStage stage) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.listOpportunities(stage)));
    }

    @GetMapping("/opportunities/{id}")
    @PreAuthorize("hasAnyAuthority('crm:read','crm:opportunity')")
    public ResponseEntity<ApiResponse<OpportunityResponse>> getOpportunity(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.getOpportunity(id)));
    }

    @PostMapping("/opportunities")
    @PreAuthorize("hasAuthority('crm:opportunity')")
    public ResponseEntity<ApiResponse<OpportunityResponse>> createOpportunity(
            @Valid @RequestBody OpportunityRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Opportunity created", commercialService.createOpportunity(req)));
    }

    @PutMapping("/opportunities/{id}")
    @PreAuthorize("hasAuthority('crm:opportunity')")
    public ResponseEntity<ApiResponse<OpportunityResponse>> updateOpportunity(
            @PathVariable Long id, @Valid @RequestBody OpportunityRequest req) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.updateOpportunity(id, req)));
    }

    @PatchMapping("/opportunities/{id}/stage")
    @PreAuthorize("hasAuthority('crm:opportunity')")
    public ResponseEntity<ApiResponse<OpportunityResponse>> changeStage(
            @PathVariable Long id, @Valid @RequestBody OpportunityStageRequest req) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.changeStage(id, req)));
    }

    // ─── Quotations ──────────────────────────────────────────────

    @GetMapping("/quotations")
    @PreAuthorize("hasAnyAuthority('crm:read','crm:quotation')")
    public ResponseEntity<ApiResponse<List<QuotationResponse>>> listQuotations(
            @RequestParam(required = false) QuotationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.listQuotations(status)));
    }

    @GetMapping("/quotations/{id}")
    @PreAuthorize("hasAnyAuthority('crm:read','crm:quotation')")
    public ResponseEntity<ApiResponse<QuotationResponse>> getQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.getQuotation(id)));
    }

    @PostMapping("/quotations")
    @PreAuthorize("hasAuthority('crm:quotation')")
    public ResponseEntity<ApiResponse<QuotationResponse>> createQuotation(
            @Valid @RequestBody QuotationRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Quotation created", commercialService.createQuotation(req)));
    }

    @PutMapping("/quotations/{id}")
    @PreAuthorize("hasAuthority('crm:quotation')")
    public ResponseEntity<ApiResponse<QuotationResponse>> updateQuotation(
            @PathVariable Long id, @Valid @RequestBody QuotationRequest req) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.updateQuotation(id, req)));
    }

    @PostMapping("/quotations/{id}/submit")
    @PreAuthorize("hasAuthority('crm:quotation')")
    public ResponseEntity<ApiResponse<QuotationResponse>> submitQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.submitQuotation(id)));
    }

    @PostMapping("/quotations/{id}/approve")
    @PreAuthorize("hasAuthority('crm:quotation_approve')")
    public ResponseEntity<ApiResponse<QuotationResponse>> approveQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.approveQuotation(id)));
    }

    @PostMapping("/quotations/{id}/send")
    @PreAuthorize("hasAuthority('crm:quotation')")
    public ResponseEntity<ApiResponse<QuotationResponse>> sendQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.sendQuotation(id)));
    }

    @PostMapping("/quotations/{id}/accept")
    @PreAuthorize("hasAuthority('crm:quotation')")
    public ResponseEntity<ApiResponse<QuotationResponse>> acceptQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.acceptQuotation(id)));
    }

    @PostMapping("/quotations/{id}/reject")
    @PreAuthorize("hasAuthority('crm:quotation')")
    public ResponseEntity<ApiResponse<QuotationResponse>> rejectQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.rejectQuotation(id)));
    }

    @PostMapping("/quotations/{id}/cancel")
    @PreAuthorize("hasAuthority('crm:quotation')")
    public ResponseEntity<ApiResponse<QuotationResponse>> cancelQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.cancelQuotation(id)));
    }

    // ─── Tasks / Activities ───────────────────────────────────────

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyAuthority('crm:read','crm:task')")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> listTasks(
            @RequestParam(required = false) CrmTaskStatus status) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.listTasks(status)));
    }

    @PostMapping("/tasks")
    @PreAuthorize("hasAuthority('crm:task')")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody TaskRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Task created", commercialService.createTask(req)));
    }

    @PutMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('crm:task')")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id, @Valid @RequestBody TaskRequest req) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.updateTask(id, req)));
    }

    @GetMapping("/activities")
    @PreAuthorize("hasAuthority('crm:read')")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> listActivities(
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(ApiResponse.success(commercialService.listActivities(clientId)));
    }

    @PostMapping("/activities")
    @PreAuthorize("hasAnyAuthority('crm:create','crm:update','crm:lead','crm:task')")
    public ResponseEntity<ApiResponse<ActivityResponse>> createActivity(
            @Valid @RequestBody ActivityRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Activity recorded", commercialService.createActivity(req)));
    }

    // ─── Dashboard / pipeline ────────────────────────────────────

    @GetMapping("/pipeline")
    @PreAuthorize("hasAuthority('crm:read')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPipeline() {
        return ResponseEntity.ok(ApiResponse.success(crmService.getPipelineSummary()));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('crm:read','crm:report')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> stats = new java.util.LinkedHashMap<>(crmService.getDashboardStats());
        stats.putAll(commercialService.commercialDashboard());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/reports/commercial")
    @PreAuthorize("hasAuthority('crm:report')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> commercialReport() {
        return ResponseEntity.ok(ApiResponse.success(commercialService.commercialDashboard()));
    }

    // ─── Farm visits ─────────────────────────────────────────────

    @GetMapping("/farm-visits")
    @PreAuthorize("hasAuthority('crm:read')")
    public ResponseEntity<ApiResponse<List<FarmVisitResponse>>> getVisits(
            @RequestParam(required = false) Long clientId) {
        if (clientId != null) {
            return ResponseEntity.ok(ApiResponse.success(crmService.getVisitsByClient(clientId)));
        }
        return ResponseEntity.ok(ApiResponse.success(crmService.getAllVisits()));
    }

    @PostMapping("/farm-visits")
    @PreAuthorize("hasAuthority('crm:create')")
    public ResponseEntity<ApiResponse<FarmVisitResponse>> createVisit(@Valid @RequestBody FarmVisitRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Visit recorded", crmService.createVisit(req)));
    }

    @GetMapping("/follow-ups")
    @PreAuthorize("hasAuthority('crm:read')")
    public ResponseEntity<ApiResponse<List<FarmVisitResponse>>> getDueFollowUps() {
        return ResponseEntity.ok(ApiResponse.success(crmService.getDueFollowUps()));
    }
}
