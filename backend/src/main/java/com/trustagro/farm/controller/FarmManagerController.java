package com.trustagro.farm.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.farm.dto.EligibleSiteManagerResponse;
import com.trustagro.farm.dto.SiteFarmManagerRequest;
import com.trustagro.farm.dto.SiteFarmManagerResponse;
import com.trustagro.farm.service.FarmManagerService;
import com.trustagro.user.entity.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/farm-managers")
@RequiredArgsConstructor
public class FarmManagerController {

    private final FarmManagerService farmManagerService;

    @GetMapping
    @PreAuthorize("hasAuthority('farm:read')")
    public ResponseEntity<ApiResponse<List<SiteFarmManagerResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(farmManagerService.getAllSiteManagers()));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('farm:create', 'farm:update')")
    public ResponseEntity<ApiResponse<SiteFarmManagerResponse>> create(@Valid @RequestBody SiteFarmManagerRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Site farm manager created successfully", farmManagerService.createSiteFarmManager(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<SiteFarmManagerResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody SiteFarmManagerRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Site farm manager updated successfully", farmManagerService.updateSiteFarmManager(id, req)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<SiteFarmManagerResponse>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        UserStatus status = UserStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(ApiResponse.success("Site farm manager status updated", farmManagerService.updateStatus(id, status)));
    }

    @GetMapping("/eligible")
    @PreAuthorize("hasAuthority('farm:read')")
    public ResponseEntity<ApiResponse<List<EligibleSiteManagerResponse>>> getEligible(
            @RequestParam(required = false) Long farmId) {
        return ResponseEntity.ok(ApiResponse.success(farmManagerService.getEligibleSiteManagers(farmId)));
    }
}
