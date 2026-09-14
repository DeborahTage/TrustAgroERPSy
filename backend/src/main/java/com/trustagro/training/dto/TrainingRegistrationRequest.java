package com.trustagro.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TrainingRegistrationRequest {
    @NotNull
    private Long sessionId;
    /** Existing CRM client. If null, create/find by phone+name. */
    private Long clientId;
    @NotBlank
    private String participantName;
    private String participantPhone;
    private String participantEmail;
    private String organization;
    private String jobTitle;
    private BigDecimal discountAmount;
    private boolean applyTax;
    private String notes;
}
