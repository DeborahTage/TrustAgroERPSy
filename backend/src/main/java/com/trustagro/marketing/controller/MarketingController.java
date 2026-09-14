package com.trustagro.marketing.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.marketing.entity.MarketingCampaign;
import com.trustagro.marketing.entity.MarketingLead;
import com.trustagro.marketing.service.MarketingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketing")
@RequiredArgsConstructor
public class MarketingController {

    private final MarketingService marketingService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('marketing:read','crm:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(marketingService.getDashboard()));
    }

    @GetMapping("/campaigns")
    @PreAuthorize("hasAuthority('marketing:read')")
    public ResponseEntity<ApiResponse<List<MarketingCampaign>>> campaigns() {
        return ResponseEntity.ok(ApiResponse.success(marketingService.getCampaigns()));
    }

    @PostMapping("/campaigns")
    @PreAuthorize("hasAuthority('marketing:create')")
    public ResponseEntity<ApiResponse<MarketingCampaign>> createCampaign(@RequestBody MarketingCampaign campaign) {
        return ResponseEntity.ok(ApiResponse.success("Campaign created", marketingService.createCampaign(campaign)));
    }

    @GetMapping("/leads")
    @PreAuthorize("hasAnyAuthority('marketing:read','crm:read')")
    public ResponseEntity<ApiResponse<List<MarketingLead>>> leads() {
        return ResponseEntity.ok(ApiResponse.success(marketingService.getLeads()));
    }

    @PostMapping("/leads")
    @PreAuthorize("hasAuthority('marketing:create')")
    public ResponseEntity<ApiResponse<MarketingLead>> createLead(@RequestBody MarketingLead lead) {
        return ResponseEntity.ok(ApiResponse.success("Lead captured", marketingService.createLead(lead)));
    }

    @PostMapping("/leads/{id}/convert")
    @PreAuthorize("hasAnyAuthority('marketing:update','crm:create')")
    public ResponseEntity<ApiResponse<MarketingLead>> convertLead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lead converted to CRM", marketingService.convertLeadToCrm(id)));
    }
}
