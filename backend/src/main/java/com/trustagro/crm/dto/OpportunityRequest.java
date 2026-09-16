package com.trustagro.crm.dto;

import com.trustagro.crm.entity.CrmLeadSource;
import com.trustagro.crm.entity.CrmPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OpportunityRequest {
    @NotNull private Long clientId;
    @NotBlank private String title;
    private String description;
    private BigDecimal estimatedValue;
    private String currency;
    private Integer probability;
    private LocalDate expectedCloseDate;
    private Long salesOwnerId;
    private CrmLeadSource source;
    private CrmPriority priority;
    private String notes;
    private Long leadId;
}
