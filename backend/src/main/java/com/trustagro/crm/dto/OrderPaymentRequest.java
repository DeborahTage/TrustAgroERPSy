package com.trustagro.crm.dto;

import com.trustagro.finance.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OrderPaymentRequest {
    @NotNull private BigDecimal amount;
    @NotNull private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    /** Optional; duplicate keys return the existing payment's order response. */
    private String idempotencyKey;
}
