package com.trustagro.consultancy.dto;

import com.trustagro.consultancy.entity.DeliverableStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DeliverableResponse {
    private Long id;
    private String deliverableNumber;
    private Long engagementId;
    private String engagementNumber;
    private String title;
    private String description;
    private LocalDate dueDate;
    private LocalDate submittedDate;
    private LocalDate approvedDate;
    private DeliverableStatus status;
    private Long assignedToId;
    private String assignedToName;
    private BigDecimal billableAmount;
    private boolean billable;
    private Long invoiceId;
    private String invoiceNumber;
    private String notes;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
