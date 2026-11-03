package com.trustagro.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trustagro.common.exception.GlobalExceptionHandler;
import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.repository.FinanceAuditLogRepository;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.finance.service.InvoiceService;
import com.trustagro.finance.service.ReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FinanceControllerValidationTest {

    private FinanceService financeService;
    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        financeService = mock(FinanceService.class);
        FinanceController controller = new FinanceController(
                financeService,
                mock(ReconciliationService.class),
                mock(FinanceAuditLogRepository.class),
                mock(InvoiceService.class));
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void invalidRequestsReturnStructuredBadRequestWithoutPosting() throws Exception {
        String[][] invalidRequests = {
                {"""
                {"transactionType":"INCOME","amount":12.50,"creditAccountCode":"REV-100"}
                """, "debitAccountCode"},
                {"""
                {"transactionType":"INCOME","amount":12.50,"debitAccountCode":"CASH-100"}
                """, "creditAccountCode"},
                {"""
                {"amount":12.50,"debitAccountCode":"CASH-100","creditAccountCode":"REV-100"}
                """, "transactionType"},
                {"""
                {"transactionType":"INCOME","amount":0,"debitAccountCode":"CASH-100","creditAccountCode":"REV-100"}
                """, "amount"}
        };

        for (String[] invalidRequest : invalidRequests) {
            mockMvc.perform(post("/api/finance/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest[0]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.data").isMap())
                    .andExpect(jsonPath("$.data." + invalidRequest[1]).exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        verify(financeService, never()).postTransaction(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void validRequestReturnsTransactionResponseAndOk() throws Exception {
        FinanceTransaction transaction = new FinanceTransaction();
        transaction.setId(42L);
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setCategory("MANUAL");
        transaction.setAmount(new BigDecimal("125.50"));
        transaction.setReferenceType("MANUAL");
        transaction.setDescription("Manual revenue entry");
        when(financeService.postTransaction(
                eq(TransactionType.INCOME),
                eq(new BigDecimal("125.50")),
                any(),
                eq("CASH-100"),
                eq("REV-100"),
                eq("Manual revenue entry"),
                eq("MANUAL"),
                eq("MANUAL"),
                eq(null)))
                .thenReturn(transaction);

        mockMvc.perform(post("/api/finance/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transactionType":"INCOME",
                                  "amount":125.50,
                                  "debitAccountCode":"CASH-100",
                                  "creditAccountCode":"REV-100",
                                  "referenceType":"MANUAL",
                                  "description":"Manual revenue entry"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.transactionType").value("INCOME"))
                .andExpect(jsonPath("$.data.amount").value(125.50))
                .andExpect(jsonPath("$.data.debitAccountCode").value("CASH-100"))
                .andExpect(jsonPath("$.data.creditAccountCode").value("REV-100"))
                .andExpect(jsonPath("$.data.referenceType").value("MANUAL"))
                .andExpect(jsonPath("$.data.description").value("Manual revenue entry"))
                .andExpect(jsonPath("$.data.recordedBy").doesNotExist());

        verify(financeService).postTransaction(
                TransactionType.INCOME,
                new BigDecimal("125.50"),
                com.trustagro.finance.entity.BusinessUnit.GENERAL,
                "CASH-100",
                "REV-100",
                "Manual revenue entry",
                "MANUAL",
                "MANUAL",
                null);
    }
}
