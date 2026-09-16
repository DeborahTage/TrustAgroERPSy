package com.trustagro.consultancy.dto;

import com.trustagro.consultancy.entity.AssignmentRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AssignmentRequest {
    @NotNull
    private Long consultantId;
    private AssignmentRole role;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
    /** When true, set as engagement lead consultant. */
    private boolean asLead = true;
}
