package com.trustagro.training.dto;

import com.trustagro.finance.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TrainingPaymentRequest {
    @NotNull
    private BigDecimal amount;
    @NotNull
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private String notes;
}
