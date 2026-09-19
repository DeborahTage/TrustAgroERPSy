package com.trustagro.farm.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DailyFeedUsageLineRequest {
    private Long inventoryItemId;
    private String feedType;
    @PositiveOrZero
    private BigDecimal issuedQuantity;
    @PositiveOrZero
    private BigDecimal returnedQuantity;
    private String unit;
    private String notes;
}
