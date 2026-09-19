package com.trustagro.farm.delivery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InternalStockDeliveryRequest {
    private LocalDate deliveryDate;
    @NotNull
    private Long inventoryItemId;
    @Size(max = 20)
    private String unit;
    @NotNull
    @Positive
    private BigDecimal quantity;
    private Long delivererId;
    private Long receiverId;
    @Size(max = 120)
    private String sourceLocation;
    @Size(max = 120)
    private String destinationLocation;
    @Size(max = 200)
    private String purpose;
    private String notes;
}
