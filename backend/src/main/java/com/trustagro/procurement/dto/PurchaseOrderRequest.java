package com.trustagro.procurement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderRequest {
    @NotNull
    private Long supplierId;
    private LocalDate orderDate;
    private LocalDate expectedDelivery;
    private String paymentTerms;
    private String currency;
    private BigDecimal taxAmount;
    private String notes;
    private Long requisitionId;
    @NotEmpty
    @Valid
    private List<Line> items;

    @Data
    public static class Line {
        @NotNull
        private Long inventoryItemId;
        @NotNull
        private Double quantityOrdered;
        @NotNull
        private BigDecimal unitPrice;
        private String notes;
    }
}
