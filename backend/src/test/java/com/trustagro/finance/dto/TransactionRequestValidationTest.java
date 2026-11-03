package com.trustagro.finance.dto;

import com.trustagro.finance.entity.TransactionType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRequestValidationTest {

    private static Validator validator;
    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void validPairedPostingRequestPassesValidation() {
        TransactionRequest request = request();

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void missingEitherAccountFailsValidation() {
        TransactionRequest request = request();
        request.setDebitAccountCode(" ");

        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("debitAccountCode")));

        request.setDebitAccountCode("1000");
        request.setCreditAccountCode(null);
        assertTrue(validator.validate(request).stream()
                .anyMatch(error -> error.getPropertyPath().toString().equals("creditAccountCode")));
    }

    @Test
    void nonPositiveOrOverPrecisionAmountsFailValidation() {
        TransactionRequest request = request();
        request.setAmount(BigDecimal.ZERO);
        assertFalse(validator.validate(request).isEmpty());

        request.setAmount(new BigDecimal("-0.01"));
        assertFalse(validator.validate(request).isEmpty());

        request.setAmount(new BigDecimal("1.001"));
        assertFalse(validator.validate(request).isEmpty());

        request.setAmount(BigDecimal.TEN.pow(36));
        assertFalse(validator.validate(request).isEmpty());
    }

    private TransactionRequest request() {
        TransactionRequest request = new TransactionRequest();
        request.setTransactionType(TransactionType.INCOME);
        request.setAmount(new BigDecimal("12.34"));
        request.setDebitAccountCode("1000");
        request.setCreditAccountCode("4100");
        return request;
    }
}
