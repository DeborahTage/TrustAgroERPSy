package com.trustagro.farm.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DailyMedicationLineResponse {
    private Long id;
    private Long inventoryItemId;
    private String inventoryItemName;
    private String treatmentType;
    private BigDecimal quantity;
    private String unit;
    private String notes;
    private Long stockMovementId;
    private boolean inventoryAlreadyPosted;
}
