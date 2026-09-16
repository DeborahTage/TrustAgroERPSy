package com.trustagro.crm.dto;

import com.trustagro.crm.entity.CrmActivityType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActivityResponse {
    private Long id;
    private CrmActivityType type;
    private String subject;
    private String body;
    private Long clientId;
    private Long leadId;
    private Long opportunityId;
    private Long performedById;
    private String performedByName;
    private LocalDateTime activityDate;
    private LocalDateTime createdAt;
}
