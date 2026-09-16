package com.trustagro.hr.dto;

import com.trustagro.finance.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PayrollPaymentRequest {
    /** Settle a single employee's net pay. Omit to disburse the whole run. */
    private Long payrollItemId;
    /** Defaults to the full outstanding net of the item or run. */
    @DecimalMin("0.0")
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private String notes;
}
