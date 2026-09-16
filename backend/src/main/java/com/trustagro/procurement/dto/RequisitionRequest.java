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
public class RequisitionRequest {
    @NotBlank
    private String title;
    private String justification;
    private LocalDate neededBy;
    private String priority; // ProcRequisitionPriority name
    @NotEmpty
    @Valid
    private List<RequisitionItemRequest> items;

    @Data
    public static class RequisitionItemRequest {
        @NotNull
        private Long inventoryItemId;
        @NotNull
        private Double quantityRequested;
        private BigDecimal estimatedUnitPrice;
        private String notes;
    }
}
