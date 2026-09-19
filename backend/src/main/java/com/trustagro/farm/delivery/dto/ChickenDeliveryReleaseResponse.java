package com.trustagro.farm.delivery.dto;

import com.trustagro.farm.delivery.entity.ChickenReleaseStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ChickenDeliveryReleaseResponse {
    private Long id;
    private String releaseNumber;
    private Long chickenSaleId;
    private String saleNumber;
    private Long chickenSalePaymentId;
    private Long invoiceId;
    private Long farmId;
    private String farmName;
    private Long flockId;
    private String flockBatchCode;
    private Long customerId;
    private String customerName;
    private BigDecimal paymentAmount;
    private Integer authorizedChickenQuantity;
    private Integer bonusQuantity;
    private String chickenType;
    private String paymentReceiptUrl;
    private ChickenReleaseStatus status;
    private String verifiedByName;
    private LocalDateTime verifiedAt;
    private String farmApprovedByName;
    private LocalDateTime farmApprovedAt;
    private String farmRejectionReason;
    private String dispatchedByName;
    private LocalDateTime dispatchedAt;
    private LocalDate dispatchDate;
    private String marketingNotes;
    private String deliveryOutcome;
    private boolean stockDecremented;
    private String notes;
    private String createdByName;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
