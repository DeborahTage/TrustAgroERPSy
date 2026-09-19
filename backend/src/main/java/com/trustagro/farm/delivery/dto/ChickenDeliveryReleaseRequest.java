package com.trustagro.farm.delivery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChickenDeliveryReleaseRequest {
    @NotNull
    private Long chickenSaleId;
    private Long chickenSalePaymentId;
    private Long invoiceId;
    private BigDecimal paymentAmount;
    @NotNull
    @Positive
    private Integer authorizedChickenQuantity;
    private Integer bonusQuantity;
    @Size(max = 80)
    private String chickenType;
    @Size(max = 500)
    private String paymentReceiptUrl;
    private String notes;
}
