package com.trustagro.hr.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LeaveBalanceResponse {
    private Long id;
    private Long employeeId;
    private String employeeNumber;
    private String employeeName;
    private Long leaveTypeId;
    private String leaveTypeCode;
    private String leaveTypeName;
    private int year;
    private BigDecimal entitlement;
    private BigDecimal usedDays;
    private BigDecimal remainingDays;
}
