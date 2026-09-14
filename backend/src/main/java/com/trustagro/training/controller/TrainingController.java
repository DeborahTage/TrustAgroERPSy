package com.trustagro.training.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.training.dto.*;
import com.trustagro.training.entity.ProgramStatus;
import com.trustagro.training.service.TrainingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('training:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(trainingService.dashboard()));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyAuthority('training:report','training:read','reports:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reports() {
        return ResponseEntity.ok(ApiResponse.success(trainingService.reports()));
    }

    // Programs
    @GetMapping("/programs")
    @PreAuthorize("hasAuthority('training:read')")
    public ResponseEntity<ApiResponse<List<TrainingProgramResponse>>> listPrograms() {
        return ResponseEntity.ok(ApiResponse.success(trainingService.listPrograms()));
    }

    @GetMapping("/programs/{id}")
    @PreAuthorize("hasAuthority('training:read')")
    public ResponseEntity<ApiResponse<TrainingProgramResponse>> getProgram(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.getProgram(id)));
    }

    @PostMapping("/programs")
    @PreAuthorize("hasAuthority('training:create')")
    public ResponseEntity<ApiResponse<TrainingProgramResponse>> createProgram(@Valid @RequestBody TrainingProgramRequest req) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.createProgram(req)));
    }

    @PutMapping("/programs/{id}")
    @PreAuthorize("hasAuthority('training:update')")
    public ResponseEntity<ApiResponse<TrainingProgramResponse>> updateProgram(
            @PathVariable Long id, @Valid @RequestBody TrainingProgramRequest req) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.updateProgram(id, req)));
    }

    @PostMapping("/programs/{id}/publish")
    @PreAuthorize("hasAuthority('training:publish')")
    public ResponseEntity<ApiResponse<TrainingProgramResponse>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.transitionProgram(id, ProgramStatus.PUBLISHED)));
    }

    @PostMapping("/programs/{id}/open-registration")
    @PreAuthorize("hasAuthority('training:publish')")
    public ResponseEntity<ApiResponse<TrainingProgramResponse>> openRegistration(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.transitionProgram(id, ProgramStatus.REGISTRATION_OPEN)));
    }

    @PostMapping("/programs/{id}/close-registration")
    @PreAuthorize("hasAuthority('training:publish')")
    public ResponseEntity<ApiResponse<TrainingProgramResponse>> closeRegistration(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.transitionProgram(id, ProgramStatus.REGISTRATION_CLOSED)));
    }

    @PostMapping("/programs/{id}/start")
    @PreAuthorize("hasAuthority('training:update')")
    public ResponseEntity<ApiResponse<TrainingProgramResponse>> start(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.transitionProgram(id, ProgramStatus.IN_PROGRESS)));
    }

    @PostMapping("/programs/{id}/complete")
    @PreAuthorize("hasAuthority('training:complete')")
    public ResponseEntity<ApiResponse<TrainingProgramResponse>> completeProgram(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.transitionProgram(id, ProgramStatus.COMPLETED)));
    }

    @PostMapping("/programs/{id}/cancel")
    @PreAuthorize("hasAuthority('training:cancel')")
    public ResponseEntity<ApiResponse<TrainingProgramResponse>> cancelProgram(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.transitionProgram(id, ProgramStatus.CANCELLED)));
    }

    // Sessions
    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('training:read')")
    public ResponseEntity<ApiResponse<List<TrainingSessionResponse>>> listSessions(
            @RequestParam(required = false) Long programId) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.listSessions(programId)));
    }

    @GetMapping("/sessions/{id}")
    @PreAuthorize("hasAuthority('training:read')")
    public ResponseEntity<ApiResponse<TrainingSessionResponse>> getSession(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.getSession(id)));
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasAuthority('training:create')")
    public ResponseEntity<ApiResponse<TrainingSessionResponse>> createSession(@Valid @RequestBody TrainingSessionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.createSession(req)));
    }

    // Registrations
    @GetMapping("/registrations")
    @PreAuthorize("hasAuthority('training:read')")
    public ResponseEntity<ApiResponse<List<TrainingRegistrationResponse>>> listRegistrations(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.listRegistrations(sessionId, clientId)));
    }

    @GetMapping("/registrations/{id}")
    @PreAuthorize("hasAuthority('training:read')")
    public ResponseEntity<ApiResponse<TrainingRegistrationResponse>> getRegistration(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.getRegistration(id)));
    }

    @PostMapping("/registrations")
    @PreAuthorize("hasAuthority('training:register')")
    public ResponseEntity<ApiResponse<TrainingRegistrationResponse>> register(@Valid @RequestBody TrainingRegistrationRequest req) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.register(req)));
    }

    @PostMapping("/registrations/{id}/confirm")
    @PreAuthorize("hasAuthority('training:register')")
    public ResponseEntity<ApiResponse<TrainingRegistrationResponse>> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.confirmRegistration(id)));
    }

    @PostMapping("/registrations/{id}/cancel")
    @PreAuthorize("hasAuthority('training:cancel')")
    public ResponseEntity<ApiResponse<TrainingRegistrationResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.cancelRegistration(id)));
    }

    @PostMapping("/registrations/{id}/payments")
    @PreAuthorize("hasAuthority('training:payment')")
    public ResponseEntity<ApiResponse<TrainingRegistrationResponse>> pay(
            @PathVariable Long id, @Valid @RequestBody TrainingPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.recordPayment(id, req)));
    }

    @PostMapping("/registrations/{id}/attendance")
    @PreAuthorize("hasAuthority('training:attendance')")
    public ResponseEntity<ApiResponse<TrainingRegistrationResponse>> attendance(
            @PathVariable Long id, @Valid @RequestBody AttendanceRequest req) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.recordAttendance(id, req)));
    }

    @PostMapping("/registrations/{id}/complete")
    @PreAuthorize("hasAuthority('training:complete')")
    public ResponseEntity<ApiResponse<TrainingRegistrationResponse>> completeRegistration(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.markCompleted(id)));
    }

    @PostMapping("/registrations/{id}/certificate")
    @PreAuthorize("hasAuthority('training:certificate')")
    public ResponseEntity<ApiResponse<TrainingRegistrationResponse>> issueCertificate(
            @PathVariable Long id, @RequestBody(required = false) CertificateIssueRequest req) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.issueCertificate(id, req)));
    }

    @PostMapping("/registrations/{id}/certificate/revoke")
    @PreAuthorize("hasAuthority('training:certificate')")
    public ResponseEntity<ApiResponse<TrainingRegistrationResponse>> revokeCertificate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.revokeCertificate(id)));
    }

    @GetMapping("/certificates")
    @PreAuthorize("hasAuthority('training:read')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> certificates() {
        return ResponseEntity.ok(ApiResponse.success(trainingService.listCertificates()));
    }
}
