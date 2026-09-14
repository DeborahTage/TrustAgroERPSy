package com.trustagro.training.dto;

import com.trustagro.training.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AttendanceRequest {
    @NotNull
    private LocalDate sessionDate;
    @NotNull
    private AttendanceStatus status;
    private String notes;
}
