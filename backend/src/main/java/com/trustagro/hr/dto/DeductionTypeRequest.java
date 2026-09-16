package com.trustagro.hr.dto;

import com.trustagro.hr.entity.DeductionCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DeductionTypeRequest {
    private String code;
    @NotBlank
    private String name;
    private DeductionCategory category;
    /** Percent of gross. Configured by the organisation — the ERP applies no statutory schedule. */
    @DecimalMin("0.0")
    private BigDecimal ratePercent;
    @DecimalMin("0.0")
    private BigDecimal fixedAmount;
    private Boolean active;
    private String notes;
}
