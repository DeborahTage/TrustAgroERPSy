package com.trustagro.farm.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.farm.entity.Barn;
import com.trustagro.farm.service.BarnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farms/barns")
@RequiredArgsConstructor
public class BarnController {

    private final BarnService barnService;

    @GetMapping
    @PreAuthorize("hasAuthority('farm:read')")
    public ResponseEntity<ApiResponse<List<Barn>>> getAllBarns() {
        return ResponseEntity.ok(ApiResponse.success(barnService.getAllBarns()));
    }

    @GetMapping("/farm/{farmId}")
    @PreAuthorize("hasAuthority('farm:read')")
    public ResponseEntity<ApiResponse<List<Barn>>> getBarnsByFarm(@PathVariable Long farmId) {
        return ResponseEntity.ok(ApiResponse.success(barnService.getBarnsByFarm(farmId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('farm:create')")
    public ResponseEntity<ApiResponse<Barn>> createBarn(@RequestBody Barn barn) {
        return ResponseEntity.ok(ApiResponse.success("Barn created", barnService.createBarn(barn)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<Barn>> updateBarn(@PathVariable Long id, @RequestBody Barn barn) {
        return ResponseEntity.ok(ApiResponse.success("Barn updated", barnService.updateBarn(id, barn)));
    }
}
