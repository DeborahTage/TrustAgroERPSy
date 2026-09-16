package com.trustagro.crm.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrderRequest {
    @NotNull private Long clientId;
    private Long quotationId;
    private Long opportunityId;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private String currency;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private String deliveryAddress;
    private String contactNumber;
    private String notes;
    private List<OrderItemRequest> items;
}
