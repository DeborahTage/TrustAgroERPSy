package com.trustagro.compliance.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.compliance.entity.HaccpChecklist;
import com.trustagro.compliance.entity.HaccpLog;
import com.trustagro.compliance.service.TraceabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final TraceabilityService traceabilityService;

    @GetMapping("/haccp/checklists")
    @PreAuthorize("hasAnyAuthority('farm:read','veterinary:read')")
    public ResponseEntity<ApiResponse<List<HaccpChecklist>>> getChecklists() {
        // Mock data return or repository call
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/haccp/logs")
    @PreAuthorize("hasAnyAuthority('farm:update','veterinary:create')")
    public ResponseEntity<ApiResponse<HaccpLog>> logCheck(@RequestBody HaccpLog log) {
        return ResponseEntity.ok(ApiResponse.success("Compliance log recorded", null));
    }

    @GetMapping("/trace/{flockId}")
    @PreAuthorize("hasAnyAuthority('farm:read','veterinary:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTraceability(@PathVariable Long flockId) {
        return ResponseEntity.ok(ApiResponse.success(traceabilityService.getBatchTrace(flockId)));
    }
}
