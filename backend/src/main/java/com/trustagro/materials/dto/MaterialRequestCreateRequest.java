package com.trustagro.materials.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class MaterialRequestCreateRequest {
    @NotNull
    private String type; // PURCHASE_REQUEST | INTERNAL_ISSUE_REQUEST
    private String department;
    private Long farmId;
    private LocalDate requestDate;
    private LocalDate neededBy;
    private String priority;
    private String reason;
    private String notes;
    private String attachmentUrl;
    private Long preferredSupplierId;

    @NotEmpty
    @Valid
    private List<LineRequest> lines;

    @Data
    public static class LineRequest {
        @NotNull
        private Long inventoryItemId;
        @NotNull
        private BigDecimal quantityRequested;
        private String unit;
        private BigDecimal estimatedUnitPrice;
        private String purpose;
        private String notes;
    }
}
