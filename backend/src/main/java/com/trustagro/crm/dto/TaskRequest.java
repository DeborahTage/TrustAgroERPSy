package com.trustagro.crm.dto;

import com.trustagro.crm.entity.CrmPriority;
import com.trustagro.crm.entity.CrmTaskStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequest {
    @NotBlank private String title;
    private String description;
    private Long clientId;
    private Long leadId;
    private Long opportunityId;
    private Long assignedUserId;
    private LocalDate dueDate;
    private CrmPriority priority;
    private CrmTaskStatus status;
}
