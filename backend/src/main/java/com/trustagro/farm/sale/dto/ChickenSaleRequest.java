package com.trustagro.farm.sale.dto;

import com.trustagro.farm.sale.entity.ChickenSaleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChickenSaleRequest {
    @NotNull
    private Long farmId;
    private Long barnId;
    @NotNull
    private Long flockId;
    @NotNull
    private Long customerId;
    @NotNull
    private LocalDate saleDate;
    @NotNull
    private ChickenSaleType saleType;
    private String notes;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;

    @Valid
    @NotEmpty
    private List<ChickenSaleItemRequest> items = new ArrayList<>();
}
