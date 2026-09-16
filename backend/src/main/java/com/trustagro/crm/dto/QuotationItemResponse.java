package com.trustagro.crm.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class QuotationItemResponse {
    private Long id;
    private String description;
    private String productOrService;
    private Double quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal lineTotal;
}
