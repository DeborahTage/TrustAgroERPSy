package com.trustagro.hr.dto;

import com.trustagro.hr.entity.PayrollRunStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PayrollRunResponse {
    private Long id;
    private Long payrollPeriodId;
    private String periodCode;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private int runNumber;
    private PayrollRunStatus status;
    private int employeeCount;
    private BigDecimal grossAmount;
    private BigDecimal deductionAmount;
    private BigDecimal netAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private String currency;
    private String createdByName;
    private String approvedByName;
    private String processedByName;
    private LocalDateTime approvedAt;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
