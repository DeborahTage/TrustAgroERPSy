package com.trustagro.materials.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DisbursementRequest {
    private String disbursementNumber;
    private Long supplierId;
    @NotNull
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDate paymentDate;
    private String evidenceUrl;
}
