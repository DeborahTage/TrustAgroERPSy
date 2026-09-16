package com.trustagro.crm.dto;

import com.trustagro.crm.entity.QuotationStatus;
import com.trustagro.crm.entity.QuotationType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuotationResponse {
    private Long id;
    private String quotationNumber;
    private Long clientId;
    private String clientName;
    private Long opportunityId;
    private QuotationType quotationType;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private QuotationStatus status;
    private String currency;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String notes;
    private Long createdById;
    private Long approvedById;
    private Long convertedOrderId;
    private Long consultancyRequestId;
    private Long consultancyEngagementId;
    private Long consultancyContractId;
    private List<QuotationItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
