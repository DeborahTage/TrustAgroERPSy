package com.trustagro.farm.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DailyMedicationLineRequest {
    private Long inventoryItemId;
    private String treatmentType;
    @PositiveOrZero
    private BigDecimal quantity;
    private String unit;
    private String notes;
    private boolean inventoryAlreadyPosted;
}
