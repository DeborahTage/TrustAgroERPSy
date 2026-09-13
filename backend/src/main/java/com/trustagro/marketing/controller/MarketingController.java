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
    @PreAuthorize("hasAnyRole('ADMIN','MARKETING','CRM_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(marketingService.getDashboard()));
    }

    @GetMapping("/campaigns")
    @PreAuthorize("hasAnyRole('ADMIN','MARKETING')")
    public ResponseEntity<ApiResponse<List<MarketingCampaign>>> campaigns() {
        return ResponseEntity.ok(ApiResponse.success(marketingService.getCampaigns()));
    }

    @PostMapping("/campaigns")
    @PreAuthorize("hasAnyRole('ADMIN','MARKETING')")
    public ResponseEntity<ApiResponse<MarketingCampaign>> createCampaign(@RequestBody MarketingCampaign campaign) {
        return ResponseEntity.ok(ApiResponse.success("Campaign created", marketingService.createCampaign(campaign)));
    }

    @GetMapping("/leads")
    @PreAuthorize("hasAnyRole('ADMIN','MARKETING','CRM_MANAGER')")
    public ResponseEntity<ApiResponse<List<MarketingLead>>> leads() {
        return ResponseEntity.ok(ApiResponse.success(marketingService.getLeads()));
    }

    @PostMapping("/leads")
    @PreAuthorize("hasAnyRole('ADMIN','MARKETING')")
    public ResponseEntity<ApiResponse<MarketingLead>> createLead(@RequestBody MarketingLead lead) {
        return ResponseEntity.ok(ApiResponse.success("Lead captured", marketingService.createLead(lead)));
    }

    @PostMapping("/leads/{id}/convert")
    @PreAuthorize("hasAnyRole('ADMIN','MARKETING','CRM_MANAGER')")
    public ResponseEntity<ApiResponse<MarketingLead>> convertLead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lead converted to CRM", marketingService.convertLeadToCrm(id)));
    }
}
