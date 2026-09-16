package com.trustagro.crm.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private Long id;
    private String productName;
    private String category;
    private Long flockId;
    private Double quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
