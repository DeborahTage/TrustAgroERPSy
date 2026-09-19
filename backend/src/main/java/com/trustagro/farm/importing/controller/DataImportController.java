package com.trustagro.farm.importing.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.farm.importing.dto.DataImportRequest;
import com.trustagro.farm.importing.dto.DataImportResult;
import com.trustagro.farm.importing.service.DataImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/data-import")
@RequiredArgsConstructor
public class DataImportController {

    private final DataImportService dataImportService;

    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('farm:read') or hasAuthority('inventory:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> templates() {
        return ResponseEntity.ok(ApiResponse.success(dataImportService.templates()));
    }

    @GetMapping("/onboarding-summary")
    @PreAuthorize("hasAuthority('farm:read') or hasAuthority('inventory:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> onboardingSummary() {
        return ResponseEntity.ok(ApiResponse.success(dataImportService.onboardingSummary()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farm:create') or hasAuthority('inventory:create')")
    public ResponseEntity<ApiResponse<DataImportResult>> importData(@RequestBody DataImportRequest req) {
        return ResponseEntity.ok(ApiResponse.success(dataImportService.importData(req)));
    }
}
