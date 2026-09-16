package com.trustagro.hr.dto;

import com.trustagro.hr.entity.EmploymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeStatusRequest {
    @NotNull
    private EmploymentStatus status;
    private LocalDate effectiveDate;
    private String reason;
}
