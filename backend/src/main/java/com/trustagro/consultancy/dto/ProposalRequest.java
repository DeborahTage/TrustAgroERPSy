package com.trustagro.consultancy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProposalRequest {
    @NotNull
    private Long customerId;
    private Long requestId;
    private Long serviceId;
    @NotBlank
    private String title;
    private String scope;
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal proposedAmount;
    private String currency;
    private LocalDate validUntil;
    private String notes;
}
