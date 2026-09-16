package com.trustagro.crm.dto;

import com.trustagro.crm.entity.QuotationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class QuotationRequest {
    @NotNull private Long clientId;
    private Long opportunityId;
    private QuotationType quotationType;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private String currency;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private String notes;
    @Valid private List<QuotationItemRequest> items;
}
