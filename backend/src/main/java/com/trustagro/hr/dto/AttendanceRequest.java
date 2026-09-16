package com.trustagro.hr.dto;

import com.trustagro.hr.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceRequest {
    @NotNull
    private Long employeeId;
    @NotNull
    private LocalDate attendanceDate;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private AttendanceStatus status;
    private String notes;
}
