package com.trustagro.consultancy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DeliverableRequest {
    @NotNull
    private Long engagementId;
    @NotBlank
    private String title;
    private String description;
    private LocalDate dueDate;
    private Long assignedToId;
    private BigDecimal billableAmount;
    private Boolean billable;
    private String notes;
}
