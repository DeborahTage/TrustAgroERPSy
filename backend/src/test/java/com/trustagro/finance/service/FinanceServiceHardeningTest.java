package com.trustagro.finance.service;

import com.trustagro.finance.entity.*;
import com.trustagro.finance.repository.ChartOfAccountRepository;
import com.trustagro.finance.repository.FinanceAuditLogRepository;
import com.trustagro.finance.repository.FinanceTransactionRepository;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceServiceHardeningTest {

    @Mock FinanceTransactionRepository transactionRepository;
    @Mock ChartOfAccountRepository chartOfAccountRepository;
    @Mock FinanceAuditLogRepository auditLogRepository;
    @Mock UserRepository userRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock com.trustagro.audit.service.AuditService auditService;

    @InjectMocks FinanceService financeService;

    private final AtomicLong ids = new AtomicLong(1);
    private ChartOfAccount cash;
    private ChartOfAccount ar;
    private ChartOfAccount revenue;
    private ChartOfAccount inventory;
    private ChartOfAccount expense;
    private ChartOfAccount ap;
    private ChartOfAccount cogs;

    @BeforeEach
    void seedAccounts() {
        cash = account("1000", AccountType.ASSET);
        ar = account("1200", AccountType.ASSET);
        revenue = account("4100", AccountType.REVENUE);
        inventory = account("1400", AccountType.ASSET);
        expense = account("5120", AccountType.EXPENSE);
        ap = account("2000", AccountType.LIABILITY);
        cogs = account("5010", AccountType.EXPENSE);

        lenient().when(chartOfAccountRepository.findByAccountCode("1000")).thenReturn(Optional.of(cash));
        lenient().when(chartOfAccountRepository.findByAccountCode("1200")).thenReturn(Optional.of(ar));
        lenient().when(chartOfAccountRepository.findByAccountCode("4100")).thenReturn(Optional.of(revenue));
        lenient().when(chartOfAccountRepository.findByAccountCode("1400")).thenReturn(Optional.of(inventory));
        lenient().when(chartOfAccountRepository.findByAccountCode("5120")).thenReturn(Optional.of(expense));
        lenient().when(chartOfAccountRepository.findByAccountCode("2000")).thenReturn(Optional.of(ap));
        lenient().when(chartOfAccountRepository.findByAccountCode("5010")).thenReturn(Optional.of(cogs));
        lenient().when(chartOfAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(transactionRepository.save(any())).thenAnswer(inv -> {
            FinanceTransaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(ids.getAndIncrement());
            }
            return t;
        });
        lenient().when(transactionRepository.findBySourceModuleAndReferenceTypeAndReferenceId(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void chickenPartialPaymentDoesNotPostAsIncome() {
        // Invoice: Dr AR 75k / Cr Revenue 75k
        financeService.postTransaction(TransactionType.INCOME, new BigDecimal("75000"),
                BusinessUnit.POULTRY_FARM, "1200", "4100",
                "Chicken invoice", "CHICKEN_SALE", "INVOICE_REVENUE", 1L);
        // Payment: Dr Cash 30k / Cr AR 30k — TRANSFER only
        financeService.postTransaction(TransactionType.TRANSFER, new BigDecimal("30000"),
                BusinessUnit.POULTRY_FARM, "1000", "1200",
                "Partial payment", "CHICKEN_SALE", "PAYMENT", 10L);

        ArgumentCaptor<FinanceTransaction> cap = ArgumentCaptor.forClass(FinanceTransaction.class);
        verify(transactionRepository, times(2)).save(cap.capture());
        List<FinanceTransaction> posted = cap.getAllValues();
        assertEquals(TransactionType.INCOME, posted.get(0).getTransactionType());
        assertEquals(0, new BigDecimal("75000.00").compareTo(posted.get(0).getAmount()));
        assertEquals(TransactionType.TRANSFER, posted.get(1).getTransactionType());
        assertEquals(0, new BigDecimal("30000.00").compareTo(posted.get(1).getAmount()));
        assertNotEquals(TransactionType.INCOME, posted.get(1).getTransactionType());

        // AR balance: +75k -30k = 45k
        assertEquals(0, new BigDecimal("45000.00").compareTo(ar.getBalance()));
        assertEquals(0, new BigDecimal("30000.00").compareTo(cash.getBalance()));
        assertEquals(0, new BigDecimal("75000.00").compareTo(revenue.getBalance()));
    }

    @Test
    void duplicateSourceReferenceIsIdempotent() {
        FinanceTransaction first = new FinanceTransaction();
        first.setId(99L);
        first.setAmount(new BigDecimal("5000.00"));
        when(transactionRepository.findBySourceModuleAndReferenceTypeAndReferenceId(
                "PHARMACY", "PHARMACY_SALE_REVENUE", 5L)).thenReturn(Optional.of(first));

        FinanceTransaction result = financeService.postTransaction(TransactionType.INCOME,
                new BigDecimal("5000"), BusinessUnit.PHARMACY, "1000", "4100",
                "retry", "PHARMACY", "PHARMACY_SALE_REVENUE", 5L);

        assertEquals(99L, result.getId());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void internalConsumptionCreditsInventoryNotCash() {
        financeService.postInventoryConsumption(new BigDecimal("500"), BusinessUnit.PHARMACY, "5120",
                "Vet Rx", "VETERINARY", "VETERINARY_PRESCRIPTION", 7L);

        ArgumentCaptor<FinanceTransaction> cap = ArgumentCaptor.forClass(FinanceTransaction.class);
        verify(transactionRepository).save(cap.capture());
        FinanceTransaction t = cap.getValue();
        assertEquals(TransactionType.EXPENSE, t.getTransactionType());
        assertEquals(inventory, t.getCreditAccount());
        assertEquals(expense, t.getDebitAccount());
        assertNotEquals(cash, t.getCreditAccount());
    }

    @Test
    void inventoryReceiptIsApNotExpense() {
        financeService.postInventoryReceipt(new BigDecimal("1000"), "Stock in",
                "INVENTORY", "STOCK_IN", 3L);

        ArgumentCaptor<FinanceTransaction> cap = ArgumentCaptor.forClass(FinanceTransaction.class);
        verify(transactionRepository).save(cap.capture());
        assertEquals(TransactionType.TRANSFER, cap.getValue().getTransactionType());
        assertEquals(inventory, cap.getValue().getDebitAccount());
        assertEquals(ap, cap.getValue().getCreditAccount());
    }

    @Test
    void dashboardUsesInvoiceOutstandingForAr() {
        when(transactionRepository.sumByType(TransactionType.INCOME)).thenReturn(new BigDecimal("75000"));
        when(transactionRepository.sumByType(TransactionType.EXPENSE)).thenReturn(new BigDecimal("10000"));
        when(transactionRepository.sumByBusinessUnitAndType(any(), any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.findAll()).thenReturn(List.of());
        when(invoiceRepository.sumOutstandingOpen()).thenReturn(new BigDecimal("45000"));
        when(invoiceRepository.countOpen()).thenReturn(1L);
        when(transactionRepository.sumDebitsToAccount(any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumCreditsFromAccount(any())).thenReturn(BigDecimal.ZERO);

        Map<String, Object> dash = financeService.getDashboardSummary();
        assertEquals(0, new BigDecimal("45000").compareTo((BigDecimal) dash.get("accountsReceivable")));
        assertEquals(0, new BigDecimal("75000").compareTo((BigDecimal) dash.get("totalRevenue")));
        assertTrue(dash.get("accountingNote").toString().contains("TRANSFER"));
    }

    private ChartOfAccount account(String code, AccountType type) {
        ChartOfAccount a = new ChartOfAccount();
        a.setId(ids.getAndIncrement());
        a.setAccountCode(code);
        a.setAccountName(code);
        a.setAccountType(type);
        a.setBalance(BigDecimal.ZERO);
        return a;
    }
}
