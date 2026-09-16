package com.trustagro.procurement.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class GoodsReceiptRequest {
    @NotNull
    private Long purchaseOrderId;
    private LocalDate receiptDate;
    private String notes;
    @NotEmpty
    @Valid
    private List<Line> items;

    @Data
    public static class Line {
        @NotNull
        private Long purchaseOrderItemId;
        @NotNull
        private Double receivedQty;
        private Double rejectedQty;
        private String batchNumber;
        private LocalDate expiryDate;
        private LocalDate manufactureDate;
        private BigDecimal unitCost;
    }
}
