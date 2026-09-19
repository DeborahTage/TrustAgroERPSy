package com.trustagro.farm.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DailyFeedUsageLineResponse {
    private Long id;
    private Long inventoryItemId;
    private String inventoryItemName;
    private String feedType;
    private BigDecimal issuedQuantity;
    private BigDecimal returnedQuantity;
    private BigDecimal consumedQuantity;
    private String unit;
    private String notes;
    private Long stockMovementId;
}
