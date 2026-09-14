package com.trustagro.veterinary.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.veterinary.dto.*;
import com.trustagro.veterinary.service.VeterinaryService;
import com.trustagro.veterinary.service.VeterinaryDashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vet")
@RequiredArgsConstructor
public class VeterinaryController {

    private final VeterinaryService vetService;
    private final VeterinaryDashboardService dashboardService;
    private final com.trustagro.veterinary.service.VeterinaryReportService reportService;
    private final com.trustagro.veterinary.service.PrescriptionWorkflowService prescriptionWorkflow;

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAuthority('veterinary:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardData()));
    }

    @GetMapping("/vaccinations")
    @PreAuthorize("hasAuthority('veterinary:read')")
    public ResponseEntity<ApiResponse<List<VaccinationResponse>>> getVaccinations() {
        return ResponseEntity.ok(ApiResponse.success(vetService.getAllVaccinations()));
    }

    @PostMapping("/vaccinations")
    @PreAuthorize("hasAuthority('veterinary:create')")
    public ResponseEntity<ApiResponse<VaccinationResponse>> createVaccination(@Valid @RequestBody VaccinationRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Vaccination scheduled", vetService.createVaccination(req)));
    }

    @PatchMapping("/vaccinations/{id}/complete")
    @PreAuthorize("hasAuthority('veterinary:update')")
    public ResponseEntity<ApiResponse<VaccinationResponse>> completeVaccination(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Vaccination completed", vetService.completeVaccination(id)));
    }

    @GetMapping("/flocks/{flockId}/emr")
    @PreAuthorize("hasAuthority('veterinary:read')")
    public FlockEMRResponse getFlockEMR(@PathVariable Long flockId) {
        return vetService.getFlockEMR(flockId);
    }

    @PostMapping("/observations")
    @PreAuthorize("hasAuthority('veterinary:create')")
    public FlockObservationResponse logObservation(@RequestBody FlockObservationRequest req) {
        return vetService.logObservation(req);
    }

    @PostMapping("/necropsies")
    @PreAuthorize("hasAuthority('veterinary:create')")
    public NecropsyResponse createNecropsy(@RequestBody NecropsyRequest req) {
        return vetService.createNecropsy(req);
    }

    @PostMapping("/flocks/{flockId}/withdrawal")
    @PreAuthorize("hasAuthority('veterinary:update')")
    public void setWithdrawal(@PathVariable Long flockId, @RequestParam Integer days) {
        vetService.setFlockWithdrawal(flockId, days);
    }

    @GetMapping("/disease-cases")
    @PreAuthorize("hasAuthority('veterinary:read')")
    public ResponseEntity<ApiResponse<List<DiseaseCaseResponse>>> getDiseaseCases() {
        return ResponseEntity.ok(ApiResponse.success(vetService.getAllDiseaseCases()));
    }

    @GetMapping("/disease-cases/active")
    @PreAuthorize("hasAuthority('veterinary:read')")
    public ResponseEntity<ApiResponse<List<DiseaseCaseResponse>>> getActiveDiseaseCases() {
        return ResponseEntity.ok(ApiResponse.success(vetService.getActiveDiseaseCases()));
    }

    @GetMapping("/health-reports")
    @PreAuthorize("hasAuthority('veterinary:read')")
    public ResponseEntity<ApiResponse<List<HealthIssueReportResponse>>> getHealthReports() {
        return ResponseEntity.ok(ApiResponse.success(vetService.getAllHealthIssueReports()));
    }

    @PostMapping("/health-reports")
    @PreAuthorize("hasAnyAuthority('veterinary:create','farm:update')")
    public ResponseEntity<ApiResponse<HealthIssueReportResponse>> createHealthReport(@Valid @RequestBody HealthIssueReportRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Health issue report created", vetService.createHealthIssueReport(req)));
    }

    @PatchMapping("/health-reports/{id}/review")
    @PreAuthorize("hasAuthority('veterinary:update')")
    public ResponseEntity<ApiResponse<HealthIssueReportResponse>> reviewHealthReport(@PathVariable Long id,
                                                                                      @RequestBody HealthIssueReportReviewRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Health issue report reviewed", vetService.reviewHealthIssueReport(id, req)));
    }

    @PostMapping("/disease-cases")
    @PreAuthorize("hasAnyAuthority('veterinary:create','farm:update')")
    public ResponseEntity<ApiResponse<DiseaseCaseResponse>> createDiseaseCase(@Valid @RequestBody DiseaseCaseRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Disease case recorded", vetService.createDiseaseCase(req)));
    }

    @PutMapping("/disease-cases/{id}")
    @PreAuthorize("hasAuthority('veterinary:update')")
    public ResponseEntity<ApiResponse<DiseaseCaseResponse>> updateDiseaseCase(@PathVariable Long id, @Valid @RequestBody DiseaseCaseRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Disease case updated", vetService.updateDiseaseCase(id, req)));
    }

    @GetMapping("/treatments")
    @PreAuthorize("hasAuthority('veterinary:read')")
    public ResponseEntity<ApiResponse<List<TreatmentResponse>>> getTreatments() {
        return ResponseEntity.ok(ApiResponse.success(vetService.getAllTreatments()));
    }

    @PostMapping("/treatments")
    @PreAuthorize("hasAuthority('veterinary:create')")
    public ResponseEntity<ApiResponse<TreatmentResponse>> createTreatment(@Valid @RequestBody TreatmentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Treatment recorded", vetService.createTreatment(req)));
    }

    @GetMapping("/prescriptions")
    @PreAuthorize("hasAnyAuthority('veterinary:read','pharmacy:read')")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getPrescriptions() {
        return ResponseEntity.ok(ApiResponse.success(vetService.getAllPrescriptions()));
    }

    @GetMapping("/prescriptions/{id}")
    @PreAuthorize("hasAnyAuthority('veterinary:read','pharmacy:read','pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getPrescription(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionWorkflow.getById(id)));
    }

    @PostMapping("/prescriptions")
    @PreAuthorize("hasAuthority('veterinary:prescribe')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> createPrescription(@Valid @RequestBody PrescriptionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Prescription created", vetService.createPrescription(req)));
    }

    @PutMapping("/prescriptions/{id}")
    @PreAuthorize("hasAuthority('veterinary:prescribe')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> updateDraft(
            @PathVariable Long id, @Valid @RequestBody PrescriptionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionWorkflow.updateDraft(id, req)));
    }

    @PostMapping("/prescriptions/{id}/submit")
    @PreAuthorize("hasAuthority('veterinary:prescribe')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> submitPrescription(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Prescription submitted", prescriptionWorkflow.submit(id)));
    }

    @PostMapping("/prescriptions/{id}/cancel")
    @PreAuthorize("hasAuthority('veterinary:prescribe')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> cancelPrescription(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Prescription cancelled", prescriptionWorkflow.cancel(id)));
    }

    @PatchMapping("/prescriptions/{id}/approve")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> approvePrescription(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(vetService.approvePrescription(id)));
    }

    @PostMapping("/prescriptions/{id}/reject")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> rejectPrescription(
            @PathVariable Long id, @Valid @RequestBody RejectPrescriptionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(prescriptionWorkflow.reject(id, req)));
    }

    @PatchMapping("/prescriptions/{id}/dispense")
    @PreAuthorize("hasAuthority('pharmacy:dispense')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> dispensePrescription(
            @PathVariable Long id,
            @RequestBody(required = false) DispensePrescriptionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Prescription dispensed",
                vetService.dispensePrescription(id, req)));
    }

    @GetMapping("/reports/drug-usage")
    @PreAuthorize("hasAnyAuthority('veterinary:read','farm:read')")
    public ResponseEntity<ApiResponse<DrugUsageReportResponse>> getDrugUsageReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long flockId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generateDrugUsageReport(startDate, endDate, flockId)));
    }
}
