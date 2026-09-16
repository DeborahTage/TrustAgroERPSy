package com.trustagro.consultancy.dto;

import com.trustagro.consultancy.entity.BillingType;
import com.trustagro.consultancy.entity.ConsultancyPaymentStatus;
import com.trustagro.consultancy.entity.EngagementStatus;
import com.trustagro.consultancy.entity.RequestPriority;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EngagementResponse {
    private Long id;
    private String engagementNumber;
    private Long customerId;
    private String customerName;
    private Long serviceId;
    private String serviceName;
    private Long proposalId;
    private Long requestId;
    private String title;
    private String description;
    private String objectives;
    private String scope;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate actualEndDate;
    private String location;
    private EngagementStatus status;
    private RequestPriority priority;
    private Long leadConsultantId;
    private String leadConsultantName;
    private BigDecimal estimatedAmount;
    private BigDecimal agreedAmount;
    private String currency;
    private BillingType billingType;
    private BigDecimal invoicedAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private ConsultancyPaymentStatus paymentStatus;
    private Long invoiceId;
    private String invoiceNumber;
    private String notes;
    private LocalDateTime completedAt;
    private String completionNotes;
    private LocalDateTime createdAt;
    /** Financial summary */
    private BigDecimal revenueRecognized;
    private String profitNote;
}
