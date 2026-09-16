package com.trustagro.hr.dto;

import com.trustagro.hr.entity.PayrollPeriodStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PayrollPeriodResponse {
    private Long id;
    private String periodCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private PayrollPeriodStatus status;
    private String notes;
    /** Latest non-cancelled run for this period, when one exists. */
    private Long currentRunId;
    private int employeeCount;
    private BigDecimal grossAmount;
    private BigDecimal deductionAmount;
    private BigDecimal netAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private String createdByName;
    private String approvedByName;
    private String processedByName;
    private LocalDateTime calculatedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime processedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
}
