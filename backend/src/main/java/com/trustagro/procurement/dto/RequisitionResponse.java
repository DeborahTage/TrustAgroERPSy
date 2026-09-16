package com.trustagro.procurement.dto;

import com.trustagro.procurement.entity.ProcRequisitionPriority;
import com.trustagro.procurement.entity.ProcRequisitionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RequisitionResponse {
    private Long id;
    private String requisitionNumber;
    private ProcRequisitionStatus status;
    private ProcRequisitionPriority priority;
    private String title;
    private String justification;
    private LocalDate neededBy;
    private Long requestedById;
    private String requestedByName;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private Long convertedPurchaseOrderId;
    private String convertedPoNumber;
    private LocalDateTime createdAt;
    private List<Item> items;

    @Data
    @Builder
    public static class Item {
        private Long id;
        private Long inventoryItemId;
        private String itemCode;
        private String itemName;
        private Double quantityRequested;
        private BigDecimal estimatedUnitPrice;
        private String notes;
    }
}
