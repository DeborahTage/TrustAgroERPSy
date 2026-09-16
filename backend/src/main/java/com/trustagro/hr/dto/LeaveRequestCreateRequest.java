package com.trustagro.hr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestCreateRequest {
    /** Ignored for self-service submissions, which always resolve the caller's own profile. */
    private Long employeeId;
    @NotNull
    private Long leaveTypeId;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    private String reason;
}
