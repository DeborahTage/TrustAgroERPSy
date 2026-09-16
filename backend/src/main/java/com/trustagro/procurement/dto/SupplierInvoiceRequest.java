package com.trustagro.procurement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SupplierInvoiceRequest {
    @NotNull
    private Long supplierId;
    @NotBlank
    private String supplierInvoiceNumber;
    private Long purchaseOrderId;
    private Long goodsReceiptId;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private String currency;
    private BigDecimal taxAmount;
    private String notes;
    @NotEmpty
    @Valid
    private List<Line> items;

    @Data
    public static class Line {
        private Long purchaseOrderItemId;
        private Long inventoryItemId;
        private String description;
        @NotNull
        private Double quantity;
        @NotNull
        private BigDecimal unitPrice;
    }
}
