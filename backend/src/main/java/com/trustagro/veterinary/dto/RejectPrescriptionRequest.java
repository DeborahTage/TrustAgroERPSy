package com.trustagro.veterinary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectPrescriptionRequest {
    @NotBlank
    @Size(max = 2000)
    private String reason;
}
