package com.trustagro.consultancy.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.consultancy.dto.*;
import com.trustagro.consultancy.entity.EngagementStatus;
import com.trustagro.consultancy.entity.RequestStatus;
import com.trustagro.consultancy.service.ConsultancyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consultancy")
@RequiredArgsConstructor
public class ConsultancyController {

    private final ConsultancyService consultancyService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.dashboard()));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyAuthority('consultancy:report','consultancy:read','reports:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reports() {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.reports()));
    }

    // Services
    @GetMapping("/services")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<List<ServiceCatalogResponse>>> listServices(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.listServices(activeOnly)));
    }

    @GetMapping("/services/{id}")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> getService(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.getService(id)));
    }

    @PostMapping("/services")
    @PreAuthorize("hasAuthority('consultancy:create')")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> createService(@Valid @RequestBody ServiceCatalogRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.createService(req)));
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("hasAuthority('consultancy:update')")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> updateService(
            @PathVariable Long id, @Valid @RequestBody ServiceCatalogRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.updateService(id, req)));
    }

    // Requests
    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<List<RequestResponse>>> listRequests(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) RequestStatus status) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.listRequests(customerId, status)));
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<RequestResponse>> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.getRequest(id)));
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('consultancy:create')")
    public ResponseEntity<ApiResponse<RequestResponse>> createRequest(@Valid @RequestBody RequestCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.createRequest(req)));
    }

    @PutMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('consultancy:update')")
    public ResponseEntity<ApiResponse<RequestResponse>> updateRequest(
            @PathVariable Long id, @RequestBody RequestUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.updateRequest(id, req)));
    }

    @PostMapping("/requests/{id}/assign")
    @PreAuthorize("hasAuthority('consultancy:assign')")
    public ResponseEntity<ApiResponse<RequestResponse>> assignRequest(
            @PathVariable Long id, @Valid @RequestBody AssignmentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.assignRequest(id, req)));
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAnyAuthority('consultancy:update','consultancy:cancel')")
    public ResponseEntity<ApiResponse<RequestResponse>> rejectRequest(
            @PathVariable Long id, @RequestBody(required = false) CompleteRequest body) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.rejectRequest(id, body)));
    }

    @PostMapping("/requests/{id}/cancel")
    @PreAuthorize("hasAuthority('consultancy:cancel')")
    public ResponseEntity<ApiResponse<RequestResponse>> cancelRequest(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.cancelRequest(id)));
    }

    @PostMapping("/requests/{id}/convert-proposal")
    @PreAuthorize("hasAuthority('consultancy:create')")
    public ResponseEntity<ApiResponse<ProposalResponse>> convertToProposal(
            @PathVariable Long id, @RequestBody(required = false) ProposalRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.convertRequestToProposal(id, req)));
    }

    @PostMapping("/requests/{id}/convert-engagement")
    @PreAuthorize("hasAuthority('consultancy:create')")
    public ResponseEntity<ApiResponse<EngagementResponse>> convertToEngagement(
            @PathVariable Long id, @RequestBody(required = false) EngagementRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.convertRequestToEngagement(id, req)));
    }

    // Proposals
    @GetMapping("/proposals")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<List<ProposalResponse>>> listProposals(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long requestId) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.listProposals(customerId, requestId)));
    }

    @GetMapping("/proposals/{id}")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<ProposalResponse>> getProposal(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.getProposal(id)));
    }

    @PostMapping("/proposals")
    @PreAuthorize("hasAuthority('consultancy:create')")
    public ResponseEntity<ApiResponse<ProposalResponse>> createProposal(@Valid @RequestBody ProposalRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.createProposal(req)));
    }

    @PutMapping("/proposals/{id}")
    @PreAuthorize("hasAuthority('consultancy:update')")
    public ResponseEntity<ApiResponse<ProposalResponse>> updateProposal(
            @PathVariable Long id, @RequestBody ProposalRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.updateProposal(id, req)));
    }

    @PostMapping("/proposals/{id}/send")
    @PreAuthorize("hasAuthority('consultancy:update')")
    public ResponseEntity<ApiResponse<ProposalResponse>> sendProposal(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.sendProposal(id)));
    }

    @PostMapping("/proposals/{id}/accept")
    @PreAuthorize("hasAnyAuthority('consultancy:approve','consultancy:update')")
    public ResponseEntity<ApiResponse<ProposalResponse>> acceptProposal(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.acceptProposal(id)));
    }

    @PostMapping("/proposals/{id}/reject")
    @PreAuthorize("hasAuthority('consultancy:update')")
    public ResponseEntity<ApiResponse<ProposalResponse>> rejectProposal(
            @PathVariable Long id, @RequestBody(required = false) CompleteRequest body) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.rejectProposal(id, body)));
    }

    // Engagements
    @GetMapping("/engagements")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<List<EngagementResponse>>> listEngagements(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) EngagementStatus status) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.listEngagements(customerId, status)));
    }

    @GetMapping("/engagements/{id}")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<EngagementResponse>> getEngagement(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.getEngagement(id)));
    }

    @PostMapping("/engagements")
    @PreAuthorize("hasAuthority('consultancy:create')")
    public ResponseEntity<ApiResponse<EngagementResponse>> createEngagement(@Valid @RequestBody EngagementRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.createEngagement(req)));
    }

    @PutMapping("/engagements/{id}")
    @PreAuthorize("hasAuthority('consultancy:update')")
    public ResponseEntity<ApiResponse<EngagementResponse>> updateEngagement(
            @PathVariable Long id, @RequestBody EngagementRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.updateEngagement(id, req)));
    }

    @PostMapping("/engagements/{id}/propose")
    @PreAuthorize("hasAuthority('consultancy:update')")
    public ResponseEntity<ApiResponse<EngagementResponse>> propose(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.proposeEngagement(id)));
    }

    @PostMapping("/engagements/{id}/approve")
    @PreAuthorize("hasAuthority('consultancy:approve')")
    public ResponseEntity<ApiResponse<EngagementResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.approveEngagement(id)));
    }

    @PostMapping("/engagements/{id}/activate")
    @PreAuthorize("hasAnyAuthority('consultancy:approve','consultancy:update')")
    public ResponseEntity<ApiResponse<EngagementResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.activateEngagement(id)));
    }

    @PostMapping("/engagements/{id}/hold")
    @PreAuthorize("hasAuthority('consultancy:update')")
    public ResponseEntity<ApiResponse<EngagementResponse>> hold(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.holdEngagement(id)));
    }

    @PostMapping("/engagements/{id}/resume")
    @PreAuthorize("hasAuthority('consultancy:update')")
    public ResponseEntity<ApiResponse<EngagementResponse>> resume(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.resumeEngagement(id)));
    }

    @PostMapping("/engagements/{id}/complete")
    @PreAuthorize("hasAuthority('consultancy:complete')")
    public ResponseEntity<ApiResponse<EngagementResponse>> complete(
            @PathVariable Long id, @RequestBody(required = false) CompleteRequest body) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.completeEngagement(id, body)));
    }

    @PostMapping("/engagements/{id}/cancel")
    @PreAuthorize("hasAuthority('consultancy:cancel')")
    public ResponseEntity<ApiResponse<EngagementResponse>> cancelEngagement(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.cancelEngagement(id)));
    }

    @PostMapping("/engagements/{id}/assign")
    @PreAuthorize("hasAuthority('consultancy:assign')")
    public ResponseEntity<ApiResponse<EngagementResponse>> assignConsultant(
            @PathVariable Long id, @Valid @RequestBody AssignmentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.assignConsultant(id, req)));
    }

    @PostMapping("/engagements/{id}/invoice")
    @PreAuthorize("hasAnyAuthority('consultancy:invoice','finance:create')")
    public ResponseEntity<ApiResponse<EngagementResponse>> invoice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.createEngagementInvoice(id)));
    }

    @PostMapping("/engagements/{id}/payments")
    @PreAuthorize("hasAuthority('consultancy:payment')")
    public ResponseEntity<ApiResponse<EngagementResponse>> pay(
            @PathVariable Long id, @Valid @RequestBody PaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.recordPayment(id, req)));
    }

    // Activities
    @GetMapping("/engagements/{engagementId}/activities")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> listActivities(@PathVariable Long engagementId) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.listActivities(engagementId)));
    }

    @GetMapping("/activities/{id}")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<ActivityResponse>> getActivity(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.getActivity(id)));
    }

    @PostMapping("/activities")
    @PreAuthorize("hasAuthority('consultancy:activity')")
    public ResponseEntity<ApiResponse<ActivityResponse>> createActivity(@Valid @RequestBody ActivityRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.createActivity(req)));
    }

    @PutMapping("/activities/{id}")
    @PreAuthorize("hasAuthority('consultancy:activity')")
    public ResponseEntity<ApiResponse<ActivityResponse>> updateActivity(
            @PathVariable Long id, @RequestBody ActivityRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.updateActivity(id, req)));
    }

    @PostMapping("/activities/{id}/start")
    @PreAuthorize("hasAuthority('consultancy:activity')")
    public ResponseEntity<ApiResponse<ActivityResponse>> startActivity(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.startActivity(id)));
    }

    @PostMapping("/activities/{id}/complete")
    @PreAuthorize("hasAuthority('consultancy:activity')")
    public ResponseEntity<ApiResponse<ActivityResponse>> completeActivity(
            @PathVariable Long id, @RequestBody(required = false) CompleteRequest body) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.completeActivity(id, body)));
    }

    // Deliverables
    @GetMapping("/engagements/{engagementId}/deliverables")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<List<DeliverableResponse>>> listDeliverables(@PathVariable Long engagementId) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.listDeliverables(engagementId)));
    }

    @GetMapping("/deliverables/{id}")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<DeliverableResponse>> getDeliverable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.getDeliverable(id)));
    }

    @PostMapping("/deliverables")
    @PreAuthorize("hasAuthority('consultancy:deliverable')")
    public ResponseEntity<ApiResponse<DeliverableResponse>> createDeliverable(@Valid @RequestBody DeliverableRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.createDeliverable(req)));
    }

    @PostMapping("/deliverables/{id}/start")
    @PreAuthorize("hasAuthority('consultancy:deliverable')")
    public ResponseEntity<ApiResponse<DeliverableResponse>> startDeliverable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.startDeliverable(id)));
    }

    @PostMapping("/deliverables/{id}/submit")
    @PreAuthorize("hasAuthority('consultancy:deliverable')")
    public ResponseEntity<ApiResponse<DeliverableResponse>> submitDeliverable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.submitDeliverable(id)));
    }

    @PostMapping("/deliverables/{id}/approve")
    @PreAuthorize("hasAuthority('consultancy:approve')")
    public ResponseEntity<ApiResponse<DeliverableResponse>> approveDeliverable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.approveDeliverable(id)));
    }

    @PostMapping("/deliverables/{id}/reject")
    @PreAuthorize("hasAuthority('consultancy:approve')")
    public ResponseEntity<ApiResponse<DeliverableResponse>> rejectDeliverable(
            @PathVariable Long id, @RequestBody(required = false) CompleteRequest body) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.rejectDeliverable(id, body)));
    }

    @PostMapping("/deliverables/{id}/invoice")
    @PreAuthorize("hasAnyAuthority('consultancy:invoice','finance:create')")
    public ResponseEntity<ApiResponse<DeliverableResponse>> billMilestone(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.billMilestone(id)));
    }

    // Contracts
    @GetMapping("/engagements/{engagementId}/contract")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<ContractResponse>> getContractByEngagement(@PathVariable Long engagementId) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.getContractByEngagement(engagementId)));
    }

    @GetMapping("/contracts/{id}")
    @PreAuthorize("hasAuthority('consultancy:read')")
    public ResponseEntity<ApiResponse<ContractResponse>> getContract(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.getContract(id)));
    }

    @PostMapping("/contracts")
    @PreAuthorize("hasAuthority('consultancy:contract')")
    public ResponseEntity<ApiResponse<ContractResponse>> createContract(@Valid @RequestBody ContractRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.createContract(req)));
    }

    @PutMapping("/contracts/{id}")
    @PreAuthorize("hasAuthority('consultancy:contract')")
    public ResponseEntity<ApiResponse<ContractResponse>> updateContract(
            @PathVariable Long id, @RequestBody ContractRequest req) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.updateContract(id, req)));
    }

    @PostMapping("/contracts/{id}/activate")
    @PreAuthorize("hasAuthority('consultancy:contract')")
    public ResponseEntity<ApiResponse<ContractResponse>> activateContract(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.activateContract(id)));
    }

    @GetMapping("/clients/{clientId}/history")
    @PreAuthorize("hasAnyAuthority('consultancy:read','crm:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clientHistory(@PathVariable Long clientId) {
        return ResponseEntity.ok(ApiResponse.success(consultancyService.clientHistory(clientId)));
    }
}
