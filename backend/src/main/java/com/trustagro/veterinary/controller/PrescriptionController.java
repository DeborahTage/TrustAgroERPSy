package com.trustagro.veterinary.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.veterinary.entity.Prescription;
import com.trustagro.veterinary.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final UserRepository userRepository;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<ApiResponse<List<Prescription>>> getPending() {
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.getPendingPrescriptions()));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIAN')")
    public ResponseEntity<ApiResponse<Prescription>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.submitPrescription(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<ApiResponse<Prescription>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.approvePrescription(id)));
    }

    @PostMapping("/{id}/dispense")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<ApiResponse<Prescription>> dispense(@PathVariable Long id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(prescriptionService.dispensePrescription(id, user)));
    }
}
