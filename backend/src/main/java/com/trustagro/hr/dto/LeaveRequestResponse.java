package com.trustagro.hr.dto;

import com.trustagro.hr.entity.LeaveRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LeaveRequestResponse {
    private Long id;
    private String requestNumber;
    private Long employeeId;
    private String employeeNumber;
    private String employeeName;
    private String departmentName;
    private Long leaveTypeId;
    private String leaveTypeName;
    private boolean paid;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private String reason;
    private LeaveRequestStatus status;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
}
