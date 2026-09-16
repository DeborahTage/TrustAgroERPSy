package com.trustagro.crm.dto;

import com.trustagro.finance.entity.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderPaymentResponse {
    private Long id;
    private String paymentNumber;
    private Long orderId;
    private Long invoiceId;
    private BigDecimal amount;
    private PaymentMethod method;
    private LocalDate paymentDate;
    private Long recordedById;
    private Long financeTransactionId;
    private LocalDateTime createdAt;
}
