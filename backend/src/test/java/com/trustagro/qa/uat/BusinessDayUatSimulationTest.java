package com.trustagro.qa.uat;

import com.trustagro.finance.entity.*;
import com.trustagro.qa.fixture.QaFinanceHarness;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step 15 — realistic business-day finance/inventory/AR/AP consistency simulations.
 * ACTUALLY EXECUTED against {@link QaFinanceHarness} (real FinanceService).
 * Operational module orchestration for Day 1–3 is covered by Step 14 golden service tests;
 * this class proves the same-day ledger invariants those workflows must leave behind.
 */
class BusinessDayUatSimulationTest {

    @Test
    void day1_vetDispenseThenCrmSalePartialPayment_ledgerConsistent() {
        QaFinanceHarness h = new QaFinanceHarness();

        // 10:30 Pharmacist dispense — EXPENSE consumption, never revenue
        h.finance().postInventoryConsumption(
                new BigDecimal("200.00"), BusinessUnit.PHARMACY, "5120",
                "Day1 Rx dispense", FinanceSourceTypes.MODULE_VETERINARY,
                FinanceSourceTypes.REF_VET_DISPENSE, 701L);

        // 17:00 CRM invoice — INCOME recognition
        h.finance().postTransaction(TransactionType.INCOME, new BigDecimal("1000.00"),
                BusinessUnit.GENERAL, AccountCode.ASSET_RECEIVABLE.getCode(),
                AccountCode.REV_OTHER.getCode(), "Day1 CRM invoice",
                FinanceSourceTypes.MODULE_CRM, FinanceSourceTypes.REF_INVOICE_REVENUE, 801L);
        h.finance().postTransaction(TransactionType.TRANSFER, new BigDecimal("150.00"),
                BusinessUnit.GENERAL, AccountCode.ASSET_RECEIVABLE.getCode(),
                AccountCode.LIABILITY_VAT.getCode(), "Day1 VAT",
                FinanceSourceTypes.MODULE_CRM, FinanceSourceTypes.REF_INVOICE_VAT, 801L);

        // 17:30 Partial customer payment
        h.finance().postTransaction(TransactionType.TRANSFER, new BigDecimal("500.00"),
                BusinessUnit.GENERAL, AccountCode.ASSET_CASH.getCode(),
                AccountCode.ASSET_RECEIVABLE.getCode(), "Day1 partial pay",
                FinanceSourceTypes.MODULE_CRM, FinanceSourceTypes.REF_PAYMENT, 901L);

        assertEquals(1L, h.countByType(TransactionType.INCOME));
        assertEquals(0, new BigDecimal("1000.00").compareTo(h.sumByType(TransactionType.INCOME)));
        assertEquals(0, new BigDecimal("650.00").compareTo(h.balance(AccountCode.ASSET_RECEIVABLE.getCode())));
        assertEquals(0, new BigDecimal("500.00").compareTo(h.balance(AccountCode.ASSET_CASH.getCode())));
        assertEquals(0, new BigDecimal("200.00").compareTo(h.sumByType(TransactionType.EXPENSE)));
        h.assertNoIncomePayments();

        // Duplicate invoice revenue blocked
        h.finance().postTransaction(TransactionType.INCOME, new BigDecimal("1000.00"),
                BusinessUnit.GENERAL, AccountCode.ASSET_RECEIVABLE.getCode(),
                AccountCode.REV_OTHER.getCode(), "Day1 CRM invoice retry",
                FinanceSourceTypes.MODULE_CRM, FinanceSourceTypes.REF_INVOICE_REVENUE, 801L);
        assertEquals(1L, h.countByType(TransactionType.INCOME));
    }

    @Test
    void day2_procurementGrnApAndPartialSupplierPayment_inventoryCashAgree() {
        QaFinanceHarness h = new QaFinanceHarness();

        // 12:00 GRN — Dr Inventory / Cr AP
        h.finance().postInventoryReceipt(new BigDecimal("10000.00"), "Day2 GRN",
                FinanceSourceTypes.MODULE_PROCUREMENT, FinanceSourceTypes.REF_PROCUREMENT_GRN, 40L);

        // 13:00 Supplier invoice approve — intentionally no finance (document only)
        int afterGrn = h.ledger().size();

        // 15:00 Partial supplier payment
        h.finance().postTransaction(TransactionType.TRANSFER, new BigDecimal("4000.00"),
                BusinessUnit.GENERAL, AccountCode.LIABILITY_AP.getCode(),
                AccountCode.ASSET_BANK.getCode(), "Day2 supplier pay",
                FinanceSourceTypes.MODULE_PROCUREMENT, FinanceSourceTypes.REF_PROCUREMENT_PAYMENT, 55L);

        assertEquals(afterGrn + 1, h.ledger().size());
        assertEquals(0, new BigDecimal("10000.00").compareTo(h.balance(AccountCode.ASSET_INVENTORY.getCode())));
        assertEquals(0, new BigDecimal("6000.00").compareTo(h.balance(AccountCode.LIABILITY_AP.getCode())));
        assertEquals(0L, h.countByType(TransactionType.INCOME));
        h.assertNoIncomePayments();

        // Over-payment attempt would be rejected at service layer; source-key retry is idempotent
        h.finance().postTransaction(TransactionType.TRANSFER, new BigDecimal("4000.00"),
                BusinessUnit.GENERAL, AccountCode.LIABILITY_AP.getCode(),
                AccountCode.ASSET_BANK.getCode(), "Day2 supplier pay retry",
                FinanceSourceTypes.MODULE_PROCUREMENT, FinanceSourceTypes.REF_PROCUREMENT_PAYMENT, 55L);
        assertEquals(0, new BigDecimal("6000.00").compareTo(h.balance(AccountCode.LIABILITY_AP.getCode())));
    }

    @Test
    void day3_payrollAccrualThenDisbursement_trainingRevenue_noDoubleExpense() {
        QaFinanceHarness h = new QaFinanceHarness();

        // 12:00 Payroll process — EXPENSE
        h.finance().postTransaction(TransactionType.EXPENSE, new BigDecimal("20000.00"),
                BusinessUnit.GENERAL, AccountCode.EXP_SALARIES.getCode(),
                AccountCode.LIABILITY_SALARY_PAYABLE.getCode(), "Day3 payroll",
                "HR", "PAYROLL_GROSS", 300L);

        // 13:00 Payroll pay — TRANSFER only
        h.finance().postTransaction(TransactionType.TRANSFER, new BigDecimal("20000.00"),
                BusinessUnit.GENERAL, AccountCode.LIABILITY_SALARY_PAYABLE.getCode(),
                AccountCode.ASSET_BANK.getCode(), "Day3 payroll pay",
                "HR", "PAYROLL_PAYMENT", 400L);

        // 15:00 Training confirm revenue
        h.finance().postTransaction(TransactionType.INCOME, new BigDecimal("10000.00"),
                BusinessUnit.TRAINING, AccountCode.ASSET_RECEIVABLE.getCode(),
                AccountCode.REV_TRAINING.getCode(), "Day3 training",
                FinanceSourceTypes.MODULE_TRAINING, FinanceSourceTypes.REF_INVOICE_REVENUE, 20L);

        assertEquals(1L, h.countByType(TransactionType.EXPENSE));
        assertEquals(1L, h.countByType(TransactionType.INCOME));
        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(
                h.balance(AccountCode.LIABILITY_SALARY_PAYABLE.getCode())));
        assertEquals(0, new BigDecimal("10000.00").compareTo(h.balance(AccountCode.REV_TRAINING.getCode())));
        h.assertNoIncomePayments();
    }
}
