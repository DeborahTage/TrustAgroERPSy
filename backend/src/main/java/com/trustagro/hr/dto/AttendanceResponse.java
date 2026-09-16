package com.trustagro.hr.dto;

import com.trustagro.hr.entity.AttendanceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class AttendanceResponse {
    private Long id;
    private Long employeeId;
    private String employeeNumber;
    private String employeeName;
    private String departmentName;
    private LocalDate attendanceDate;
    private LocalTime checkIn;
    private LocalTime checkOut;
    private AttendanceStatus status;
    private BigDecimal hoursWorked;
    private String notes;
    private String recordedByName;
    private LocalDateTime createdAt;
}
