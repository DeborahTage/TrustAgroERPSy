package com.trustagro.farm.sale.dto;

import com.trustagro.farm.sale.entity.PricingMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ChickenSaleItemRequest {
    private Long flockId;

    @NotNull
    private PricingMethod pricingMethod;

    @NotNull
    private Integer quantity;

    private BigDecimal unitPrice;
    private BigDecimal weightKg;
    private BigDecimal pricePerKg;
    private String notes;
}
