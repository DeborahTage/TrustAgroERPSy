package com.trustagro.farm.delivery.dto;

import com.trustagro.farm.delivery.entity.InternalDeliveryStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InternalStockDeliveryResponse {
    private Long id;
    private String receiptNumber;
    private LocalDate deliveryDate;
    private Long inventoryItemId;
    private String inventoryItemName;
    private String inventoryItemSku;
    private String unit;
    private BigDecimal quantity;
    private Long delivererId;
    private String delivererName;
    private Long receiverId;
    private String receiverName;
    private String sourceLocation;
    private String destinationLocation;
    private String purpose;
    private String notes;
    private InternalDeliveryStatus status;
    private Long createdById;
    private String createdByName;
    private Long approvedById;
    private String approvedByName;
    private Long issuedById;
    private String issuedByName;
    private Long receivedById;
    private String receivedByName;
    private Long stockMovementId;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
