package com.trustagro.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalaryChangeRequest {
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal basicSalary;
    private String currency;
    private LocalDate effectiveDate;
    private String reason;
}
