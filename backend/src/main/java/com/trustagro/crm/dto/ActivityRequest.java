package com.trustagro.crm.dto;

import com.trustagro.crm.entity.CrmActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityRequest {
    @NotNull private CrmActivityType type;
    @NotBlank private String subject;
    private String body;
    private Long clientId;
    private Long leadId;
    private Long opportunityId;
    private LocalDateTime activityDate;
}
