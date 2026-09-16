package com.trustagro.crm.dto;

import com.trustagro.crm.entity.CrmPriority;
import com.trustagro.crm.entity.CrmTaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private Long clientId;
    private Long leadId;
    private Long opportunityId;
    private Long assignedUserId;
    private String assignedUserName;
    private LocalDate dueDate;
    private CrmPriority priority;
    private CrmTaskStatus status;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
