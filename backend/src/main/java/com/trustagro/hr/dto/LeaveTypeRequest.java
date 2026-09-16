package com.trustagro.hr.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LeaveTypeRequest {
    private String code;
    @NotBlank
    private String name;
    private String description;
    private Boolean balanceControlled;
    @DecimalMin("0.0")
    private BigDecimal defaultAnnualEntitlement;
    private Boolean paid;
    private Boolean active;
}
