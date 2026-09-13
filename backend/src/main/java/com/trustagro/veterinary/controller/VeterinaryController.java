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

    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboardData()));
    }

    @GetMapping("/vaccinations")
    public ResponseEntity<ApiResponse<List<VaccinationResponse>>> getVaccinations() {
        return ResponseEntity.ok(ApiResponse.success(vetService.getAllVaccinations()));
    }

    @PostMapping("/vaccinations")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIAN')")
    public ResponseEntity<ApiResponse<VaccinationResponse>> createVaccination(@Valid @RequestBody VaccinationRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Vaccination scheduled", vetService.createVaccination(req)));
    }

    @PatchMapping("/vaccinations/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIAN')")
    public ResponseEntity<ApiResponse<VaccinationResponse>> completeVaccination(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Vaccination completed", vetService.completeVaccination(id)));
    }

    @GetMapping("/flocks/{flockId}/emr")
    public FlockEMRResponse getFlockEMR(@PathVariable Long flockId) {
        return vetService.getFlockEMR(flockId);
    }

    @PostMapping("/observations")
    public FlockObservationResponse logObservation(@RequestBody FlockObservationRequest req) {
        return vetService.logObservation(req);
    }

    @PostMapping("/necropsies")
    public NecropsyResponse createNecropsy(@RequestBody NecropsyRequest req) {
        return vetService.createNecropsy(req);
    }

    @PostMapping("/flocks/{flockId}/withdrawal")
    public void setWithdrawal(@PathVariable Long flockId, @RequestParam Integer days) {
        vetService.setFlockWithdrawal(flockId, days);
    }

    @GetMapping("/disease-cases")
    public ResponseEntity<ApiResponse<List<DiseaseCaseResponse>>> getDiseaseCases() {
        return ResponseEntity.ok(ApiResponse.success(vetService.getAllDiseaseCases()));
    }

    @GetMapping("/disease-cases/active")
    public ResponseEntity<ApiResponse<List<DiseaseCaseResponse>>> getActiveDiseaseCases() {
        return ResponseEntity.ok(ApiResponse.success(vetService.getActiveDiseaseCases()));
    }

    @GetMapping("/health-reports")
    public ResponseEntity<ApiResponse<List<HealthIssueReportResponse>>> getHealthReports() {
        return ResponseEntity.ok(ApiResponse.success(vetService.getAllHealthIssueReports()));
    }

    @PostMapping("/health-reports")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIAN','FARM_MANAGER')")
    public ResponseEntity<ApiResponse<HealthIssueReportResponse>> createHealthReport(@Valid @RequestBody HealthIssueReportRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Health issue report created", vetService.createHealthIssueReport(req)));
    }

    @PatchMapping("/health-reports/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIAN')")
    public ResponseEntity<ApiResponse<HealthIssueReportResponse>> reviewHealthReport(@PathVariable Long id,
                                                                                      @RequestBody HealthIssueReportReviewRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Health issue report reviewed", vetService.reviewHealthIssueReport(id, req)));
    }

    @PostMapping("/disease-cases")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIAN','FARM_MANAGER')")
    public ResponseEntity<ApiResponse<DiseaseCaseResponse>> createDiseaseCase(@Valid @RequestBody DiseaseCaseRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Disease case recorded", vetService.createDiseaseCase(req)));
    }

    @PutMapping("/disease-cases/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIAN')")
    public ResponseEntity<ApiResponse<DiseaseCaseResponse>> updateDiseaseCase(@PathVariable Long id, @Valid @RequestBody DiseaseCaseRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Disease case updated", vetService.updateDiseaseCase(id, req)));
    }

    @GetMapping("/treatments")
    public ResponseEntity<ApiResponse<List<TreatmentResponse>>> getTreatments() {
        return ResponseEntity.ok(ApiResponse.success(vetService.getAllTreatments()));
    }

    @PostMapping("/treatments")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIAN')")
    public ResponseEntity<ApiResponse<TreatmentResponse>> createTreatment(@Valid @RequestBody TreatmentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Treatment recorded", vetService.createTreatment(req)));
    }

    @GetMapping("/prescriptions")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getPrescriptions() {
        return ResponseEntity.ok(ApiResponse.success(vetService.getAllPrescriptions()));
    }

    @PostMapping("/prescriptions")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIAN')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> createPrescription(@Valid @RequestBody PrescriptionRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Prescription created", vetService.createPrescription(req)));
    }

    @PatchMapping("/prescriptions/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','PHARMACIST')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> approvePrescription(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(vetService.approvePrescription(id)));
    }

    @PatchMapping("/prescriptions/{id}/dispense")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIAN','INVENTORY_MANAGER','PHARMACIST')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> dispensePrescription(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Prescription dispensed", vetService.dispensePrescription(id)));
    }

    @GetMapping("/reports/drug-usage")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIAN','FARM_MANAGER')")
    public ResponseEntity<ApiResponse<DrugUsageReportResponse>> getDrugUsageReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long flockId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.generateDrugUsageReport(startDate, endDate, flockId)));
    }
}
