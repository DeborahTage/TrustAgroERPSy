package com.trustagro.veterinary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trustagro.veterinary.entity.PrescriptionPriority;
import com.trustagro.veterinary.entity.PrescriptionStatus;
import com.trustagro.veterinary.entity.PrescriptionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PrescriptionResponse {
    private Long id;
    private String prescriptionNumber;
    private PrescriptionType prescriptionType;
    private Long farmId;
    private String farmName;
    private Long barnId;
    private Long flockId;
    private String batchCode;
    private String flockName;
    private Long clientId;
    private Long diseaseCaseId;
    private Long inventoryItemId;
    private String drugName;
    private Double quantity;
    private String unit;
    private String dosageInstruction;
    private String frequency;
    private Integer durationDays;
    private String administrationRoute;
    private Long prescribedById;
    private String createdByVet;
    private PrescriptionStatus status;
    private PrescriptionPriority priority;
    private String notes;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDate prescribedDate;
    private Integer withdrawalPeriodDays;
    private LocalDate treatmentStartDate;
    private LocalDate withdrawalEndDate;
    private Boolean flockUnderWithdrawal;
    private Double availableStock;
    private List<Map<String, Object>> availableBatches;

    private String approvedByName;
    private LocalDateTime approvedAt;
    private String dispensedByName;
    private LocalDateTime dispensedAt;
    private LocalDateTime completedAt;
    private Long dispensedBatchId;
    private String dispensedLotNumber;
    private LocalDate dispensedExpiry;
    private BigDecimal unitCostAtDispense;
    private BigDecimal totalCostAtDispense;

    /** Alias for pharmacy UI compatibility */
    @JsonProperty("prescriptionCode")
    public String getPrescriptionCode() {
        return prescriptionNumber;
    }

    @JsonProperty("prescribedBy")
    public String getPrescribedBy() {
        return createdByVet;
    }
}
