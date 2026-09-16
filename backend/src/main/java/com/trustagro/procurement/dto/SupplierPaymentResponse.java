package com.trustagro.procurement.dto;

import com.trustagro.finance.entity.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SupplierPaymentResponse {
    private Long id;
    private String paymentNumber;
    private Long supplierInvoiceId;
    private String invoiceNumber;
    private Long supplierId;
    private String supplierName;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private String notes;
    private Long recordedById;
    private String recordedByName;
    private LocalDateTime createdAt;
}
