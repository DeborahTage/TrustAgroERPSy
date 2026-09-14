package com.trustagro.veterinary.dto;

import com.trustagro.veterinary.entity.PrescriptionPriority;
import com.trustagro.veterinary.entity.PrescriptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PrescriptionRequest {
    @NotBlank @Size(max = 60)
    private String prescriptionNumber;
    private PrescriptionType prescriptionType;
    private Long farmId;
    private Long barnId;
    private Long flockId;
    private Long clientId;
    private Long diseaseCaseId;
    private Long inventoryItemId;
    @NotBlank @Size(max = 120)
    private String drugName;
    @NotNull @Positive
    private Double quantity;
    private String unit;
    @Size(max = 1200)
    private String dosageInstruction;
    private String frequency;
    private Integer durationDays;
    private String administrationRoute;
    private Integer withdrawalPeriodDays;
    private PrescriptionPriority priority;
    @Size(max = 2000)
    private String notes;
    /** When true (default), create and submit to pharmacy in one step. */
    private Boolean submitImmediately = true;
}
