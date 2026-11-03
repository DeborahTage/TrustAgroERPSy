package com.trustagro.finance.dto;

import com.trustagro.finance.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    @NotNull
    private TransactionType transactionType;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 36, fraction = 2)
    private BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    private String debitAccountCode;

    @NotBlank
    @Size(max = 255)
    private String creditAccountCode;

    @Size(max = 60)
    private String referenceType = "MANUAL";

    @Size(max = 300)
    private String description;
}
