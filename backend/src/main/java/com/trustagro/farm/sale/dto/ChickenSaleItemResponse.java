package com.trustagro.farm.sale.dto;

import com.trustagro.farm.sale.entity.PricingMethod;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChickenSaleItemResponse {
    private Long id;
    private Long flockId;
    private String flockBatchCode;
    private PricingMethod pricingMethod;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal weightKg;
    private BigDecimal pricePerKg;
    private BigDecimal lineSubtotal;
    private String notes;
}
