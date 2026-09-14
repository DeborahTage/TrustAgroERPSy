package com.trustagro.masterdata.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.masterdata.dto.MasterDataRequest;
import com.trustagro.masterdata.entity.MasterDataCategory;
import com.trustagro.masterdata.entity.MasterDataItem;
import com.trustagro.masterdata.service.MasterDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/master-data")
@RequiredArgsConstructor
public class MasterDataController {

    private final MasterDataService masterDataService;

    @GetMapping
    @PreAuthorize("hasAuthority('masterdata:read')")
    public ResponseEntity<ApiResponse<List<MasterDataItem>>> getByCategory(
            @RequestParam MasterDataCategory category,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(masterDataService.getAllByCategory(category, activeOnly)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('masterdata:update')")
    public ResponseEntity<ApiResponse<MasterDataItem>> create(@Valid @RequestBody MasterDataRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Master data created", masterDataService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:update')")
    public ResponseEntity<ApiResponse<MasterDataItem>> update(@PathVariable Long id, @Valid @RequestBody MasterDataRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Master data updated", masterDataService.update(id, request)));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('masterdata:update')")
    public ResponseEntity<ApiResponse<MasterDataItem>> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Master data status updated", masterDataService.toggleStatus(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('masterdata:update')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        masterDataService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Master data deleted", null));
    }
}
