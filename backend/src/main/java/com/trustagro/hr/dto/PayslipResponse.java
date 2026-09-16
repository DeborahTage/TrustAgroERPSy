package com.trustagro.hr.dto;

import com.trustagro.hr.entity.PayrollPaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PayslipResponse {
    private Long id;
    private String payslipNumber;
    private Long payrollItemId;
    private Long payrollRunId;
    private Long payrollPeriodId;
    private String periodCode;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private Long employeeId;
    private String employeeNumber;
    private String employeeName;
    private String departmentName;
    private String positionName;
    private BigDecimal basicSalary;
    private BigDecimal allowances;
    private BigDecimal overtime;
    private BigDecimal bonuses;
    private BigDecimal grossSalary;
    private BigDecimal tax;
    private BigDecimal pension;
    private BigDecimal otherDeductions;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private String currency;
    private PayrollPaymentStatus paymentStatus;
    private LocalDateTime createdAt;
}
