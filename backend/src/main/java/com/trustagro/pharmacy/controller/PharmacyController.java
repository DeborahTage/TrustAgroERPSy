package com.trustagro.pharmacy.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.pharmacy.dto.PosSaleRequest;
import com.trustagro.pharmacy.entity.DurAlert;
import com.trustagro.pharmacy.entity.PharmacyCustomer;
import com.trustagro.pharmacy.entity.PharmacySale;
import com.trustagro.pharmacy.repository.DurAlertRepository;
import com.trustagro.pharmacy.service.PharmacyService;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.veterinary.dto.DispensePrescriptionRequest;
import com.trustagro.veterinary.dto.PrescriptionResponse;
import com.trustagro.veterinary.dto.RejectPrescriptionRequest;
import com.trustagro.veterinary.service.PrescriptionWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Pharmacy controller. Prescription endpoints use the canonical
 * {@link PrescriptionWorkflowService} (same entity as /api/vet/prescriptions).
 * Legacy PharmacyPrescription create/approve/dispense paths are no longer exposed.
 */
@RestController
@RequestMapping("/api/pharmacy")
public class PharmacyController {

    private final PharmacyService pharmacyService;
    private final DurAlertRepository durAlertRepository;
    private final UserRepository userRepository;
    private final PrescriptionWorkflowService prescriptionWorkflow;

    public PharmacyController(PharmacyService pharmacyService,
                              DurAlertRepository durAlertRepository,
                              UserRepository userRepository,
                              PrescriptionWorkflowService prescriptionWorkflow) {
        this.pharmacyService = pharmacyService;
        this.durAlertRepository = durAlertRepository;
        this.userRepository = userRepository;
        this.prescriptionWorkflow = prescriptionWorkflow;
    }

    private User getAuthenticatedUser(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping("/prescriptions")
    @PreAuthorize("hasAnyAuthority('pharmacy:read','pharmacy:dispense')")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getPrescriptions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long farmId,
            @RequestParam(required = false) Long flockId,
            @RequestParam(required = false) Long veterinarianId) {
        return ResponseEntity.ok(ApiResponse.success(
                prescriptionWorkflow.pharmacyQueue(status, priority, farmId, flockId, veterinarianId)));
    }

    @GetMapping("/prescriptions/{id}")
    @PreAuthorize("hasAnyAuthority('pharmacy:read','pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getPrescription(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionWorkflow.getById(id)));
    }

    /** Open / review — returns canonical Rx with stock/batch details (no separate pharmacy entity). */
    @PostMapping("/prescriptions/{id}/process")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> processPrescription(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionWorkflow.getById(id)));
    }

    @PostMapping("/prescriptions/{id}/approve")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> approvePrescription(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionWorkflow.approve(id)));
    }

    @PostMapping("/prescriptions/{id}/reject")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> rejectPrescription(
            @PathVariable Long id, @Valid @RequestBody RejectPrescriptionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionWorkflow.reject(id, req)));
    }

    @PostMapping("/prescriptions/{id}/dispense")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> dispensePrescription(
            @PathVariable Long id,
            @RequestBody(required = false) DispensePrescriptionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                "Prescription dispensed successfully",
                prescriptionWorkflow.dispense(id, req)));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAnyAuthority('pharmacy:read','pharmacy:sale')")
    public ResponseEntity<List<PharmacySale>> getSales() {
        return ResponseEntity.ok(pharmacyService.getRecentSales());
    }

    @PostMapping("/sales")
    @PreAuthorize("hasAuthority('pharmacy:sale')")
    public ResponseEntity<PharmacySale> createSale(@RequestBody PosSaleRequest request, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        PharmacySale sale = pharmacyService.processSale(request, user);
        return ResponseEntity.ok(sale);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('pharmacy:read')")
    public ResponseEntity<?> getDashboardStats() {
        Map<String, Object> stats = pharmacyService.getDashboardStats();
        long pending = prescriptionWorkflow.pharmacyQueue(null, null, null, null, null).stream()
                .filter(r -> r.getStatus() != null && (
                        "PENDING_PHARMACY".equals(r.getStatus().name())
                                || "PENDING".equals(r.getStatus().name())
                                || "APPROVED".equals(r.getStatus().name())
                                || "SUBMITTED".equals(r.getStatus().name())))
                .count();
        stats.put("pending_prescriptions", pending);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('pharmacy:read')")
    public ResponseEntity<List<PharmacyCustomer>> getCustomers() {
        return ResponseEntity.ok(pharmacyService.getAllCustomers());
    }

    @PostMapping("/customers")
    @PreAuthorize("hasAnyAuthority('pharmacy:sale','pharmacy:dispense')")
    public ResponseEntity<PharmacyCustomer> createCustomer(@RequestBody PharmacyCustomer customer) {
        return ResponseEntity.ok(pharmacyService.saveCustomer(customer));
    }

    @GetMapping("/dur-alerts")
    @PreAuthorize("hasAuthority('pharmacy:read')")
    public ResponseEntity<List<DurAlert>> getDurAlerts(@RequestParam(required = false) Boolean resolved) {
        if (Boolean.FALSE.equals(resolved)) {
            return ResponseEntity.ok(durAlertRepository.findByResolvedFalse());
        }
        return ResponseEntity.ok(durAlertRepository.findAll());
    }

    @PostMapping("/labels/{rxItemId}")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<?> generateLabel(@PathVariable Long rxItemId, Authentication auth) {
        User user = getAuthenticatedUser(auth);
        return ResponseEntity.ok(pharmacyService.generateLabel(rxItemId, user));
    }
}
