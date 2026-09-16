package com.trustagro.crm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuotationItemRequest {
    @NotBlank private String description;
    private String productOrService;
    @NotNull private Double quantity;
    private String unit;
    @NotNull private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
}
