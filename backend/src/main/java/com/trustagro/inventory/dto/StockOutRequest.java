package com.trustagro.inventory.dto;

import com.trustagro.inventory.entity.IssuedToType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StockOutRequest {
    @NotNull
    private Long itemId;
    @NotNull @Positive
    private Double quantity;
    @Size(max = 160)
    private String reason;
    private IssuedToType issuedToType;
    private Long farmId;
    @Size(max = 80)
    private String department;
    @Size(max = 60)
    private String referenceType;
    private Long referenceId;
    private LocalDate movementDate;
    private Long prescriptionId;
    private Long vaccinationId;
    /** Optional FEFO override — dispense from this batch only. */
    private Long batchId;
    /** When set (e.g. INTERNAL_CONSUMPTION), overrides default ISSUE/SALE typing. */
    private String movementTypeOverride;

    public void setItemId(Long itemId) { this.itemId = itemId; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public void setReason(String reason) { this.reason = reason; }
    public void setIssuedToType(IssuedToType issuedToType) { this.issuedToType = issuedToType; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public void setPrescriptionId(Long prescriptionId) { this.prescriptionId = prescriptionId; }
    public void setVaccinationId(Long vaccinationId) { this.vaccinationId = vaccinationId; }

    public Long getItemId() { return itemId; }
    public Double getQuantity() { return quantity; }
    public String getReason() { return reason; }
    public IssuedToType getIssuedToType() { return issuedToType; }
    public Long getFarmId() { return farmId; }
    public String getReferenceType() { return referenceType; }
    public Long getReferenceId() { return referenceId; }
}
