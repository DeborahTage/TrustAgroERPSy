package com.trustagro.farm.sale.dto;

import com.trustagro.finance.entity.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ChickenSalePaymentResponse {
    private Long id;
    private Long invoiceId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private Long financeTransactionId;
    private String recordedByName;
    private String notes;
    private LocalDateTime createdAt;
}
