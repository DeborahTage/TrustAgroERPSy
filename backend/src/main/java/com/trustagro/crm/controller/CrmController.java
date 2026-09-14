package com.trustagro.crm.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.crm.dto.*;
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

    @GetMapping("/clients")
    @PreAuthorize("hasAuthority('crm:read')")
    public ResponseEntity<ApiResponse<List<CrmClientResponse>>> getClients() {
        return ResponseEntity.ok(ApiResponse.success(crmService.getAllClients()));
    }

    @GetMapping("/clients/{id}")
    @PreAuthorize("hasAuthority('crm:read')")
    public ResponseEntity<ApiResponse<CrmClientResponse>> getClient(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(crmService.getClientById(id)));
    }

    @PostMapping("/clients")
    @PreAuthorize("hasAuthority('crm:create')")
    public ResponseEntity<ApiResponse<CrmClientResponse>> createClient(@Valid @RequestBody CrmClientRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Client created", crmService.createClient(req)));
    }

    @PutMapping("/clients/{id}")
    @PreAuthorize("hasAuthority('crm:update')")
    public ResponseEntity<ApiResponse<CrmClientResponse>> updateClient(@PathVariable Long id, @Valid @RequestBody CrmClientRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Client updated", crmService.updateClient(id, req)));
    }

    @GetMapping("/leads")
    @PreAuthorize("hasAnyAuthority('crm:read','marketing:read')")
    public ResponseEntity<ApiResponse<List<CrmClientResponse>>> getLeads() {
        return ResponseEntity.ok(ApiResponse.success(crmService.getLeads()));
    }

    @GetMapping("/pipeline")
    @PreAuthorize("hasAuthority('crm:read')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPipeline() {
        return ResponseEntity.ok(ApiResponse.success(crmService.getPipelineSummary()));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('crm:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(crmService.getDashboardStats()));
    }

    @GetMapping("/farm-visits")
    @PreAuthorize("hasAuthority('crm:read')")
    public ResponseEntity<ApiResponse<List<FarmVisitResponse>>> getVisits(@RequestParam(required = false) Long clientId) {
        if (clientId != null) return ResponseEntity.ok(ApiResponse.success(crmService.getVisitsByClient(clientId)));
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
