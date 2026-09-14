package com.trustagro.training.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.training.dto.CertificateVerificationResponse;
import com.trustagro.training.service.TrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/certificates")
@RequiredArgsConstructor
public class CertificateVerificationController {

    private final TrainingService trainingService;

    @GetMapping("/verify/{code}")
    public ResponseEntity<ApiResponse<CertificateVerificationResponse>> verify(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success(trainingService.verifyCertificate(code)));
    }
}
