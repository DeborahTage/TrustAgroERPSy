package com.trustagro.crm.dto;

import com.trustagro.crm.entity.CrmLeadSource;
import com.trustagro.crm.entity.CrmLeadStatus;
import com.trustagro.crm.entity.CrmPriority;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LeadResponse {
    private Long id;
    private String leadNumber;
    private String name;
    private String company;
    private String contactPerson;
    private String phone;
    private String email;
    private CrmLeadSource source;
    private String industry;
    private String notes;
    private Long assignedOwnerId;
    private String assignedOwnerName;
    private CrmLeadStatus status;
    private CrmPriority priority;
    private LocalDateTime convertedAt;
    private Long convertedClientId;
    private Long convertedOpportunityId;
    private Long convertedById;
    private Long marketingLeadId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
