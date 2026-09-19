package com.trustagro.materials.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MaterialRequestResponse {
    private Long id;
    private String requestNumber;
    private String type;
    private String status;
    private Long requesterId;
    private String requesterName;
    private String department;
    private Long farmId;
    private String farmName;
    private LocalDate requestDate;
    private LocalDate neededBy;
    private String priority;
    private String reason;
    private String notes;
    private String attachmentUrl;
    private Long preferredSupplierId;
    private String preferredSupplierName;
    private BigDecimal estimatedTotal;
    private BigDecimal approvedAmount;
    private BigDecimal paidAmount;
    private String rejectionReason;
    private Long financeApprovedById;
    private String financeApprovedByName;
    private LocalDateTime financeApprovedAt;
    private Long procRequisitionId;
    private Long internalRequisitionId;
    private Long purchaseOrderId;
    private Long lastSupplierPaymentId;
    private Long acknowledgedById;
    private String acknowledgedByName;
    private LocalDateTime acknowledgedAt;
    private String acknowledgementNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<LineResponse> lines = new ArrayList<>();

    @Data
    public static class LineResponse {
        private Long id;
        private Long inventoryItemId;
        private String itemName;
        private String sku;
        private BigDecimal quantityRequested;
        private BigDecimal quantityApproved;
        private BigDecimal quantityOrdered;
        private BigDecimal quantityReceived;
        private BigDecimal quantityIssued;
        private String unit;
        private BigDecimal estimatedUnitPrice;
        private BigDecimal estimatedLineTotal;
        private String purpose;
        private String notes;
        private Long lastStockMovementId;
    }
}
