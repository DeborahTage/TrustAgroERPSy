package com.trustagro.procurement.dto;

import com.trustagro.inventory.entity.PurchaseOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PurchaseOrderResponse {
    private Long id;
    private String poNumber;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private PurchaseOrderStatus status;
    private LocalDate orderDate;
    private String paymentTerms;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private LocalDate expectedDelivery;
    private LocalDate actualDelivery;
    private String notes;
    private Long requisitionId;
    private String requisitionNumber;
    private Long createdById;
    private String createdByName;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Line> items;

    @Data
    @Builder
    public static class Line {
        private Long id;
        private Long inventoryItemId;
        private String itemCode;
        private String itemName;
        private Double quantityOrdered;
        private Double quantityReceived;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private String notes;
    }
}
