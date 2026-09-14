package com.trustagro.farm.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.farm.dto.FarmRequest;
import com.trustagro.farm.dto.FarmResponse;
import com.trustagro.farm.dto.BiosecurityProtocolRequest;
import com.trustagro.farm.dto.BiosecurityProtocolResponse;
import com.trustagro.farm.entity.FarmStatus;
import com.trustagro.farm.service.FarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/farms")
@RequiredArgsConstructor
public class FarmController {

    private final FarmService farmService;

    @GetMapping
    @PreAuthorize("hasAuthority('farm:read')")
    public ResponseEntity<ApiResponse<List<FarmResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(farmService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farm:read')")
    public ResponseEntity<ApiResponse<FarmResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(farmService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farm:create')")
    public ResponseEntity<ApiResponse<FarmResponse>> create(@Valid @RequestBody FarmRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Farm created", farmService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<FarmResponse>> update(@PathVariable Long id, @Valid @RequestBody FarmRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Farm updated", farmService.update(id, req)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<FarmResponse>> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(farmService.updateStatus(id, FarmStatus.valueOf(body.get("status")))));
    }

    @GetMapping("/{id}/biosecurity")
    @PreAuthorize("hasAnyAuthority('farm:read','veterinary:read')")
    public ResponseEntity<ApiResponse<List<BiosecurityProtocolResponse>>> getBiosecurityLogs(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(farmService.getBiosecurityLogs(id)));
    }

    @PostMapping("/{id}/biosecurity")
    @PreAuthorize("hasAnyAuthority('farm:update','veterinary:create')")
    public ResponseEntity<ApiResponse<BiosecurityProtocolResponse>> logBiosecurity(@PathVariable Long id, @Valid @RequestBody BiosecurityProtocolRequest req) {
        req.setFarmId(id);
        return ResponseEntity.ok(ApiResponse.success("Biosecurity log created", farmService.logBiosecurity(req)));
    }
}
