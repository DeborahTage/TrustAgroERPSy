package com.trustagro.hr.dto;

import com.trustagro.hr.entity.PayrollItemStatus;
import com.trustagro.hr.entity.PayrollPaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollItemResponse {
    private Long id;
    private Long payrollRunId;
    private Long payrollPeriodId;
    private String periodCode;
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
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private String currency;
    private PayrollItemStatus status;
    private PayrollPaymentStatus paymentStatus;
    private Long payslipId;
    private String payslipNumber;
}
