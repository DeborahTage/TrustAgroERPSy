package com.trustagro.crm.dto;

import com.trustagro.crm.entity.CrmLeadSource;
import com.trustagro.crm.entity.CrmPriority;
import com.trustagro.crm.entity.OpportunityStage;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class OpportunityResponse {
    private Long id;
    private String opportunityNumber;
    private Long clientId;
    private String clientName;
    private String title;
    private String description;
    private BigDecimal estimatedValue;
    private String currency;
    private Integer probability;
    private LocalDate expectedCloseDate;
    private Long salesOwnerId;
    private String salesOwnerName;
    private CrmLeadSource source;
    private OpportunityStage stage;
    private CrmPriority priority;
    private String lossReason;
    private String notes;
    private Long leadId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
