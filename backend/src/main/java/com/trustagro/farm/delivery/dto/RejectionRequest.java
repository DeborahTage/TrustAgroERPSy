package com.trustagro.farm.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectionRequest {
    @NotBlank
    private String reason;
}
