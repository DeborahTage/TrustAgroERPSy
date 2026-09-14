package com.trustagro.veterinary.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.veterinary.dto.DispensePrescriptionRequest;
import com.trustagro.veterinary.dto.PrescriptionResponse;
import com.trustagro.veterinary.dto.RejectPrescriptionRequest;
import com.trustagro.veterinary.service.PrescriptionWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Compatibility facade for {@code /api/prescriptions/*}.
 * All operations delegate to {@link PrescriptionWorkflowService}.
 * Prefer {@code /api/vet/prescriptions} or {@code /api/pharmacy/prescriptions}.
 */
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@Deprecated
public class PrescriptionController {

    private final PrescriptionWorkflowService workflowService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('pharmacy:read','pharmacy:dispense')")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getPending() {
        return ResponseEntity.ok(ApiResponse.success(
                workflowService.pharmacyQueue("PENDING_PHARMACY", null, null, null, null)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('veterinary:prescribe')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(workflowService.submit(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(workflowService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> reject(
            @PathVariable Long id, @Valid @RequestBody RejectPrescriptionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(workflowService.reject(id, req)));
    }

    @PostMapping("/{id}/dispense")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> dispense(
            @PathVariable Long id,
            @RequestBody(required = false) DispensePrescriptionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(workflowService.dispense(id, req)));
    }
}
