package com.trustagro.materials.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class GoodsReceiptRequest {
    private String notes;

    @NotEmpty
    @Valid
    private List<LineReceipt> lines;

    @Data
    public static class LineReceipt {
        private Long lineId;
        private Long inventoryItemId;
        private BigDecimal quantityReceived;
        private String batchNumber;
        private LocalDate expiryDate;
        private String conditionNote;
        private BigDecimal unitCost;
    }
}
