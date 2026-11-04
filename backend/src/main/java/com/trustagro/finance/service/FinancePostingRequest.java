package com.trustagro.finance.service;

import com.trustagro.finance.entity.BusinessUnit;
import com.trustagro.finance.entity.PaymentMethod;
import com.trustagro.finance.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancePostingRequest(
        TransactionType transactionType,
        BigDecimal amount,
        BusinessUnit businessUnit,
        String debitAccountCode,
        String creditAccountCode,
        String description,
        String sourceModule,
        String referenceType,
        Long referenceId,
        PaymentMethod paymentMethod,
        LocalDate transactionDate) {

    public static FinancePostingRequest of(TransactionType transactionType, BigDecimal amount,
                                           BusinessUnit businessUnit, String debitAccountCode,
                                           String creditAccountCode, String description,
                                           String sourceModule, String referenceType, Long referenceId) {
        return new FinancePostingRequest(transactionType, amount, businessUnit, debitAccountCode,
                creditAccountCode, description, sourceModule, referenceType, referenceId, null, null);
    }

    public static FinancePostingRequest of(TransactionType transactionType, BigDecimal amount,
                                           BusinessUnit businessUnit, String debitAccountCode,
                                           String creditAccountCode, String description,
                                           String sourceModule, String referenceType, Long referenceId,
                                           PaymentMethod paymentMethod, LocalDate transactionDate) {
        return new FinancePostingRequest(transactionType, amount, businessUnit, debitAccountCode,
                creditAccountCode, description, sourceModule, referenceType, referenceId,
                paymentMethod, transactionDate);
    }
}
