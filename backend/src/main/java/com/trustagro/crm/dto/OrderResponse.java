package com.trustagro.crm.dto;

import com.trustagro.crm.entity.CustomerOrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long clientId;
    private String clientName;
    private Long quotationId;
    private Long opportunityId;
    private LocalDate orderDate;
    private LocalDate expectedDeliveryDate;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private String currency;
    private CustomerOrderStatus status;
    private String deliveryAddress;
    private String contactNumber;
    private String notes;
    private Long invoiceId;
    private String invoiceNumber;
    private Long createdById;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
