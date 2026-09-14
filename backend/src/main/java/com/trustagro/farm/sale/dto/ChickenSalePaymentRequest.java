package com.trustagro.farm.sale.dto;

import com.trustagro.finance.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ChickenSalePaymentRequest {
    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private PaymentMethod paymentMethod;

    private LocalDate paymentDate;
    private String notes;
}
