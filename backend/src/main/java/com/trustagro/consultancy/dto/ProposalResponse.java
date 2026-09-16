package com.trustagro.consultancy.dto;

import com.trustagro.consultancy.entity.ProposalStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ProposalResponse {
    private Long id;
    private String proposalNumber;
    private Long customerId;
    private String customerName;
    private Long requestId;
    private String requestNumber;
    private Long serviceId;
    private String serviceName;
    private String title;
    private String scope;
    private BigDecimal proposedAmount;
    private String currency;
    private LocalDate validUntil;
    private ProposalStatus status;
    private String notes;
    private Long convertedEngagementId;
    private LocalDateTime createdAt;
}
