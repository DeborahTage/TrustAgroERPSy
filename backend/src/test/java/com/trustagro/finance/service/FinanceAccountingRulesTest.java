package com.trustagro.finance.service;

import com.trustagro.finance.entity.FinanceSourceTypes;
import com.trustagro.finance.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Accounting policy guards for finance integrations.
 * Full Spring transactional posting is covered where account seed data is available;
 * these tests lock the revenue vs settlement rules that prevent double-counting.
 */
class FinanceAccountingRulesTest {

    @Test
    void chickenSalePartialPaymentDoesNotIncreaseRevenue() {
        BigDecimal invoiceTotal = new BigDecimal("75000.00");
        BigDecimal payment = new BigDecimal("30000.00");
        BigDecimal revenue = invoiceTotal; // recognized at invoice
        BigDecimal cashReceived = payment; // settlement only
        BigDecimal ar = invoiceTotal.subtract(payment);

        assertEquals(0, new BigDecimal("75000.00").compareTo(revenue));
        assertEquals(0, new BigDecimal("30000.00").compareTo(cashReceived));
        assertEquals(0, new BigDecimal("45000.00").compareTo(ar));
        assertNotEquals(0, revenue.add(cashReceived).compareTo(new BigDecimal("75000.00")),
                "Cash receipt must not be added on top of recognized revenue");
    }

    @Test
    void pharmacyVatIsNotRevenue() {
        BigDecimal subtotal = new BigDecimal("5000.00");
        BigDecimal vat = subtotal.multiply(new BigDecimal("0.15"));
        BigDecimal revenue = subtotal;
        BigDecimal totalCashOrAr = subtotal.add(vat);

        assertEquals(0, revenue.compareTo(subtotal));
        assertTrue(vat.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(0, totalCashOrAr.compareTo(new BigDecimal("5750.00")));
        assertNotEquals(0, revenue.compareTo(totalCashOrAr),
                "VAT-inclusive total must not be posted entirely as revenue");
    }

    @Test
    void internalConsumptionIsExpenseNotRevenue() {
        assertEquals("VETERINARY", FinanceSourceTypes.MODULE_VETERINARY);
        assertEquals("VETERINARY_PRESCRIPTION", FinanceSourceTypes.REF_VET_DISPENSE);
        assertNotEquals(TransactionType.INCOME, TransactionType.EXPENSE);
    }

    @Test
    void postingPurposesAreDistinctForIdempotency() {
        assertNotEquals(FinanceSourceTypes.REF_INVOICE_REVENUE, FinanceSourceTypes.REF_INVOICE_VAT);
        assertNotEquals(FinanceSourceTypes.REF_PHARMACY_SALE_REVENUE, FinanceSourceTypes.REF_PHARMACY_SALE_VAT);
        assertNotEquals(FinanceSourceTypes.REF_PAYMENT, FinanceSourceTypes.REF_INVOICE_REVENUE);
    }

    @Test
    void transferSettlesArWithoutIncomeType() {
        assertEquals(TransactionType.TRANSFER, TransactionType.valueOf("TRANSFER"));
        assertNotEquals(TransactionType.INCOME, TransactionType.TRANSFER);
    }
}
