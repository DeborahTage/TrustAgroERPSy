package com.trustagro.procurement.dto;

import com.trustagro.finance.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SupplierPaymentRequest {
    @NotNull
    private Long supplierInvoiceId;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private String notes;
}
