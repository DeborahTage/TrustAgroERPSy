package com.trustagro.hr.dto;

import com.trustagro.finance.entity.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PayrollPaymentResponse {
    private Long id;
    private String paymentNumber;
    private Long payrollRunId;
    private Long payrollItemId;
    private Long employeeId;
    private String employeeName;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private String notes;
    private String recordedByName;
    private LocalDateTime createdAt;
}
