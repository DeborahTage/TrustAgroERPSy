package com.trustagro.consultancy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractRequest {
    @NotNull
    private Long engagementId;
    @NotBlank
    private String title;
    private String scope;
    private String terms;
    private LocalDate startDate;
    private LocalDate endDate;
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal contractValue;
    private String currency;
    private String notes;
}
