package com.trustagro.procurement.dto;

import com.trustagro.procurement.entity.GoodsReceiptStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class GoodsReceiptResponse {
    private Long id;
    private String grnNumber;
    private Long purchaseOrderId;
    private String poNumber;
    private Long supplierId;
    private String supplierName;
    private GoodsReceiptStatus status;
    private LocalDate receiptDate;
    private String notes;
    private Long receivedById;
    private String receivedByName;
    private LocalDateTime postedAt;
    private LocalDateTime createdAt;
    private List<Line> items;

    @Data
    @Builder
    public static class Line {
        private Long id;
        private Long purchaseOrderItemId;
        private Long inventoryItemId;
        private String itemCode;
        private String itemName;
        private Double receivedQty;
        private Double rejectedQty;
        private String batchNumber;
        private LocalDate expiryDate;
        private LocalDate manufactureDate;
        private BigDecimal unitCost;
        private Long stockBatchId;
    }
}
