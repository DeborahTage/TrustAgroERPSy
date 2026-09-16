package com.trustagro.hr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PayrollPeriodRequest {
    private String periodCode;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    private String notes;
}
