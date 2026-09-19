package com.trustagro.farm.delivery.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.farm.delivery.dto.*;
import com.trustagro.farm.delivery.entity.ChickenReleaseStatus;
import com.trustagro.farm.delivery.service.ChickenDeliveryReleaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chicken-releases")
@RequiredArgsConstructor
public class ChickenDeliveryReleaseController {

    private final ChickenDeliveryReleaseService releaseService;

    @GetMapping
    @PreAuthorize("hasAuthority('chickensale:read')")
    public ResponseEntity<ApiResponse<List<ChickenDeliveryReleaseResponse>>> list(
            @RequestParam(required = false) ChickenReleaseStatus status,
            @RequestParam(required = false) String queue) {
        return ResponseEntity.ok(ApiResponse.success(releaseService.list(status, queue)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('chickensale:read')")
    public ResponseEntity<ApiResponse<ChickenDeliveryReleaseResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(releaseService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('chickensale:verify') or hasAuthority('chickensale:create')")
    public ResponseEntity<ApiResponse<ChickenDeliveryReleaseResponse>> create(
            @Valid @RequestBody ChickenDeliveryReleaseRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Release submitted", releaseService.create(req)));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAuthority('chickensale:verify')")
    public ResponseEntity<ApiResponse<ChickenDeliveryReleaseResponse>> verify(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Verified", releaseService.verify(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('chickensale:verify')")
    public ResponseEntity<ApiResponse<ChickenDeliveryReleaseResponse>> reject(
            @PathVariable Long id, @Valid @RequestBody RejectionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Rejected", releaseService.reject(id, req)));
    }

    @PostMapping("/{id}/forward-to-farm")
    @PreAuthorize("hasAuthority('chickensale:verify')")
    public ResponseEntity<ApiResponse<ChickenDeliveryReleaseResponse>> forwardToFarm(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Forwarded to farm", releaseService.forwardToFarm(id)));
    }

    @PostMapping("/{id}/farm-approve")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<ChickenDeliveryReleaseResponse>> farmApprove(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Farm approved", releaseService.farmApprove(id)));
    }

    @PostMapping("/{id}/farm-reject")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<ChickenDeliveryReleaseResponse>> farmReject(
            @PathVariable Long id, @Valid @RequestBody RejectionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Farm rejected", releaseService.farmReject(id, req)));
    }

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAuthority('farm:update')")
    public ResponseEntity<ApiResponse<ChickenDeliveryReleaseResponse>> dispatch(
            @PathVariable Long id, @Valid @RequestBody ChickenDispatchRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Dispatched", releaseService.dispatch(id, req)));
    }

    @PostMapping("/{id}/marketing-receive")
    @PreAuthorize("hasAuthority('marketing:update') or hasAuthority('chickensale:read')")
    public ResponseEntity<ApiResponse<ChickenDeliveryReleaseResponse>> marketingReceive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Marketing received", releaseService.marketingReceive(id)));
    }

    @PostMapping("/{id}/logistics")
    @PreAuthorize("hasAuthority('marketing:update') or hasAuthority('chickensale:read')")
    public ResponseEntity<ApiResponse<ChickenDeliveryReleaseResponse>> updateLogistics(
            @PathVariable Long id, @RequestBody MarketingLogisticsRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Logistics updated", releaseService.updateLogistics(id, req)));
    }

    @PostMapping("/{id}/mark-delivered")
    @PreAuthorize("hasAuthority('marketing:update') or hasAuthority('chickensale:read')")
    public ResponseEntity<ApiResponse<ChickenDeliveryReleaseResponse>> markDelivered(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Marked delivered", releaseService.markDelivered(id)));
    }
}
