package com.trustagro.qa.fixture;

import com.trustagro.audit.service.AuditService;
import com.trustagro.finance.entity.AccountCode;
import com.trustagro.finance.entity.AccountType;
import com.trustagro.finance.entity.ChartOfAccount;
import com.trustagro.finance.entity.FinanceAuditLog;
import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.repository.ChartOfAccountRepository;
import com.trustagro.finance.repository.FinanceAuditLogRepository;
import com.trustagro.finance.repository.FinanceTransactionRepository;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.user.repository.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * In-memory ledger wired to a real {@link FinanceService} for cross-module QA.
 * Domain services under test receive this FinanceService; repositories stay mocked.
 */
public final class QaFinanceHarness {

    private final AtomicLong ids = new AtomicLong(1);
    private final Map<String, ChartOfAccount> accounts = new HashMap<>();
    private final List<FinanceTransaction> ledger = new ArrayList<>();
    private final Map<String, FinanceTransaction> bySourceKey = new HashMap<>();

    private final FinanceTransactionRepository transactionRepository = mock(FinanceTransactionRepository.class);
    private final ChartOfAccountRepository chartOfAccountRepository = mock(ChartOfAccountRepository.class);
    private final FinanceAuditLogRepository auditLogRepository = mock(FinanceAuditLogRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final AuditService auditService = mock(AuditService.class);

    private final FinanceService financeService;

    public QaFinanceHarness() {
        for (AccountCode code : AccountCode.values()) {
            ChartOfAccount coa = new ChartOfAccount();
            coa.setId(ids.getAndIncrement());
            coa.setAccountCode(code.getCode());
            coa.setAccountName(code.getName());
            coa.setAccountType(code.getAccountType());
            coa.setBusinessUnit(code.getUnit());
            coa.setBalance(BigDecimal.ZERO.setScale(2));
            accounts.put(code.getCode(), coa);
        }

        lenient().when(chartOfAccountRepository.findByAccountCode(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(accounts.get(inv.getArgument(0))));
        lenient().when(chartOfAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(auditLogRepository.save(any(FinanceAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        lenient().when(transactionRepository.findBySourceModuleAndReferenceTypeAndReferenceId(
                        anyString(), anyString(), any()))
                .thenAnswer(inv -> {
                    String key = key(inv.getArgument(0), inv.getArgument(1), inv.getArgument(2));
                    return Optional.ofNullable(bySourceKey.get(key));
                });
        lenient().when(transactionRepository.save(any(FinanceTransaction.class))).thenAnswer(inv -> {
            FinanceTransaction t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(ids.getAndIncrement());
            }
            ledger.add(t);
            if (t.getSourceModule() != null && t.getReferenceType() != null && t.getReferenceId() != null) {
                bySourceKey.put(key(t.getSourceModule(), t.getReferenceType(), t.getReferenceId()), t);
            }
            return t;
        });

        financeService = new FinanceService(
                transactionRepository,
                chartOfAccountRepository,
                auditLogRepository,
                userRepository,
                invoiceRepository,
                auditService);
    }

    public FinanceService finance() {
        return financeService;
    }

    public AuditService auditService() {
        return auditService;
    }

    public InvoiceRepository invoiceRepository() {
        return invoiceRepository;
    }

    public List<FinanceTransaction> ledger() {
        return List.copyOf(ledger);
    }

    public BigDecimal balance(String accountCode) {
        ChartOfAccount coa = accounts.get(accountCode);
        return coa != null && coa.getBalance() != null ? coa.getBalance() : BigDecimal.ZERO;
    }

    public BigDecimal sumByType(TransactionType type) {
        return ledger.stream()
                .filter(t -> t.getTransactionType() == type)
                .map(FinanceTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long countByType(TransactionType type) {
        return ledger.stream().filter(t -> t.getTransactionType() == type).count();
    }

    public List<FinanceTransaction> byModule(String module) {
        return ledger.stream()
                .filter(t -> module.equals(t.getSourceModule()))
                .collect(Collectors.toList());
    }

    public Map<TransactionType, BigDecimal> totalsByType() {
        Map<TransactionType, BigDecimal> map = new EnumMap<>(TransactionType.class);
        for (TransactionType type : TransactionType.values()) {
            map.put(type, sumByType(type));
        }
        return map;
    }

    public void assertNoIncomePayments() {
        List<FinanceTransaction> bad = ledger.stream()
                .filter(t -> t.getTransactionType() == TransactionType.INCOME)
                .filter(t -> t.getReferenceType() != null
                        && t.getReferenceType().toUpperCase().contains("PAYMENT"))
                .collect(Collectors.toList());
        if (!bad.isEmpty()) {
            throw new AssertionError("Payment posted as INCOME: " + bad);
        }
    }

    private static String key(String module, String refType, Long refId) {
        return module + "|" + refType + "|" + refId;
    }
}
