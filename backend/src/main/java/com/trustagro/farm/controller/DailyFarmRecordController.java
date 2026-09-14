package com.trustagro.farm.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.farm.dto.DailyFarmRecordRequest;
import com.trustagro.farm.dto.DailyFarmRecordResponse;
import com.trustagro.farm.service.DailyFarmRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/daily-farm-records")
@RequiredArgsConstructor
public class DailyFarmRecordController {

    private final DailyFarmRecordService service;

    @GetMapping
    @PreAuthorize("hasAuthority('farm:read')")
    public ResponseEntity<ApiResponse<List<DailyFarmRecordResponse>>> getAll(
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) Long flockId) {
        if (farmId != null) return ResponseEntity.ok(ApiResponse.success(service.getByFarm(farmId)));
        if (flockId != null) return ResponseEntity.ok(ApiResponse.success(service.getByFlock(flockId)));
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('farm:read')")
    public ResponseEntity<ApiResponse<DailyFarmRecordResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farm:create')")
    public ResponseEntity<ApiResponse<DailyFarmRecordResponse>> create(@Valid @RequestBody DailyFarmRecordRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Record created", service.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<DailyFarmRecordResponse>> update(@PathVariable Long id, @Valid @RequestBody DailyFarmRecordRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Record updated", service.update(id, req)));
    }
}
