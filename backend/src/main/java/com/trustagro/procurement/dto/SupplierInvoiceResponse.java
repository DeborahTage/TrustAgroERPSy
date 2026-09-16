package com.trustagro.procurement.dto;

import com.trustagro.procurement.entity.MatchStatus;
import com.trustagro.procurement.entity.SupplierInvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SupplierInvoiceResponse {
    private Long id;
    private String invoiceNumber;
    private String supplierInvoiceNumber;
    private Long supplierId;
    private String supplierName;
    private Long purchaseOrderId;
    private String poNumber;
    private Long goodsReceiptId;
    private String grnNumber;
    private SupplierInvoiceStatus status;
    private MatchStatus matchStatus;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private boolean overdue;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstanding;
    private String matchNotes;
    private String notes;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private List<Line> items;

    @Data
    @Builder
    public static class Line {
        private Long id;
        private Long purchaseOrderItemId;
        private Long inventoryItemId;
        private String itemName;
        private String description;
        private Double quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}
