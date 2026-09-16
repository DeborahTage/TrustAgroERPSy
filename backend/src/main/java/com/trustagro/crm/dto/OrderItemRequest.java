package com.trustagro.crm.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequest {
    private String productName;
    private String category;
    private Long flockId;
    private Double quantity;
    private String unit;
    private BigDecimal unitPrice;
}
