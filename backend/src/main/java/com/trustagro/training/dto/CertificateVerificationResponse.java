package com.trustagro.training.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class CertificateVerificationResponse {
    private boolean valid;
    private String status;
    private String certificateNumber;
    private String participantName;
    private String programTitle;
    private String sessionCode;
    private LocalDate issueDate;
    private String certificateType;
    private String message;
}
