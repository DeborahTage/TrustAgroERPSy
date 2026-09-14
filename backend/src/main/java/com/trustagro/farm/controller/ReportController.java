package com.trustagro.farm.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.farm.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/hub-summary")
    @PreAuthorize("hasAuthority('reports:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHubSummary() {
        Map<String, Object> hub = new HashMap<>();
        hub.put("operational", reportService.getOperationalSummary());
        hub.put("financial", reportService.getFinancialSummary());
        hub.put("inventory", reportService.getInventorySummary());
        return ResponseEntity.ok(ApiResponse.success(hub));
    }
}
