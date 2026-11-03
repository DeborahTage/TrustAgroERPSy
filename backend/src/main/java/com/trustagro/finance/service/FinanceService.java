package com.trustagro.finance.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.finance.entity.*;
import com.trustagro.finance.repository.ChartOfAccountRepository;
import com.trustagro.finance.repository.FinanceAuditLogRepository;
import com.trustagro.finance.repository.FinanceTransactionRepository;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int FINANCE_AMOUNT_PRECISION = 38;
    private static final int FINANCE_AMOUNT_SCALE = 2;

    private final FinanceTransactionRepository transactionRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final FinanceAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditService auditService;

    @Transactional
    public FinanceTransaction postRevenue(BigDecimal amount, BusinessUnit unit, String accountCode,
                                         String description, String sourceModule, String referenceType, Long referenceId) {
        return postTransaction(TransactionType.INCOME, amount, unit,
                AccountCode.ASSET_CASH.getCode(), accountCode,
                description, sourceModule, referenceType, referenceId, null, null);
    }

    @Transactional
    public FinanceTransaction postExpense(BigDecimal amount, BusinessUnit unit, String accountCode,
                                         String description, String sourceModule, String referenceType, Long referenceId) {
        return postTransaction(TransactionType.EXPENSE, amount, unit, accountCode,
                AccountCode.ASSET_CASH.getCode(),
                description, sourceModule, referenceType, referenceId, null, null);
    }

    /**
     * Internal inventory consumption: Dr expense, Cr inventory asset — never cash, never revenue.
     */
    @Transactional
    public FinanceTransaction postInventoryConsumption(BigDecimal amount, BusinessUnit unit, String expenseAccountCode,
                                                       String description, String sourceModule,
                                                       String referenceType, Long referenceId) {
        return postTransaction(TransactionType.EXPENSE, amount, unit,
                expenseAccountCode, AccountCode.ASSET_INVENTORY.getCode(),
                description, sourceModule, referenceType, referenceId, null, null);
    }

    /**
     * Stock receipt on credit: Dr inventory, Cr accounts payable — not an operating expense.
     */
    @Transactional
    public FinanceTransaction postInventoryReceipt(BigDecimal amount, String description,
                                                   String sourceModule, String referenceType, Long referenceId) {
        return postTransaction(TransactionType.TRANSFER, amount, BusinessUnit.GENERAL,
                AccountCode.ASSET_INVENTORY.getCode(), AccountCode.LIABILITY_AP.getCode(),
                description, sourceModule, referenceType, referenceId, null, null);
    }

    /**
     * Commercial COGS: Dr COGS expense, Cr inventory.
     */
    @Transactional
    public FinanceTransaction postCogs(BigDecimal amount, BusinessUnit unit, String cogsAccountCode,
                                       String description, String sourceModule, String referenceType, Long referenceId) {
        return postTransaction(TransactionType.EXPENSE, amount, unit,
                cogsAccountCode, AccountCode.ASSET_INVENTORY.getCode(),
                description, sourceModule, referenceType, referenceId, null, null);
    }

    @Transactional
    public FinanceTransaction postTransaction(TransactionType type, BigDecimal amount, BusinessUnit unit,
                                             String debitAccountCode, String creditAccountCode,
                                             String description, String sourceModule,
                                             String referenceType, Long referenceId) {
        return postTransaction(type, amount, unit, debitAccountCode, creditAccountCode,
                description, sourceModule, referenceType, referenceId, null, null);
    }

    @Transactional
    public FinanceTransaction postTransaction(TransactionType type, BigDecimal amount, BusinessUnit unit,
                                             String debitAccountCode, String creditAccountCode,
                                             String description, String sourceModule,
                                             String referenceType, Long referenceId,
                                             PaymentMethod paymentMethod, LocalDate transactionDate) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Finance amount must be greater than zero");
        }
        BigDecimal postedAmount = amount.setScale(FINANCE_AMOUNT_SCALE, RoundingMode.HALF_UP);
        if (postedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Finance amount must be greater than zero after rounding to two decimal places");
        }
        if (postedAmount.precision() > FINANCE_AMOUNT_PRECISION) {
            throw new BusinessException("Finance amount exceeds the supported precision");
        }
        if (type == null) {
            throw new BusinessException("Finance transaction type is required");
        }

        String debitCode = debitAccountCode != null ? debitAccountCode.trim() : null;
        String creditCode = creditAccountCode != null ? creditAccountCode.trim() : null;
        if (debitCode == null || debitCode.isBlank()) {
            throw new BusinessException("Debit account is required");
        }
        if (creditCode == null || creditCode.isBlank()) {
            throw new BusinessException("Credit account is required");
        }
        if (debitCode.equals(creditCode)) {
            throw new BusinessException("Debit and credit accounts must differ");
        }

        // Idempotency: same business source + posting purpose → return existing posting
        if (sourceModule != null && referenceType != null && referenceId != null) {
            Optional<FinanceTransaction> existing = transactionRepository
                    .findBySourceModuleAndReferenceTypeAndReferenceId(sourceModule, referenceType, referenceId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        ChartOfAccount debitCoa = chartOfAccountRepository.findByAccountCode(debitCode)
                .orElseThrow(() -> new ResourceNotFoundException("Debit account not found: " + debitCode));
        ChartOfAccount creditCoa = chartOfAccountRepository.findByAccountCode(creditCode)
                .orElseThrow(() -> new ResourceNotFoundException("Credit account not found: " + creditCode));

        FinanceTransaction t = new FinanceTransaction();
        t.setTransactionType(type);
        t.setAmount(postedAmount);
        t.setBusinessUnit(unit != null ? unit : BusinessUnit.GENERAL);
        t.setDescription(description);
        t.setSourceModule(sourceModule);
        t.setReferenceType(referenceType);
        t.setReferenceId(referenceId);
        t.setPaymentMethod(paymentMethod);
        t.setTransactionDate(transactionDate != null ? transactionDate : LocalDate.now());
        t.setCategory(referenceType);

        String email = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        userRepository.findByEmail(email).ifPresent(t::setRecordedBy);

        t.setDebitAccount(debitCoa);
        t.setCreditAccount(creditCoa);
        updateAccountBalance(debitCoa, t.getAmount(), true);
        updateAccountBalance(creditCoa, t.getAmount(), false);

        FinanceTransaction saved = transactionRepository.save(t);

        FinanceAuditLog log = new FinanceAuditLog();
        log.setTransactionId(saved.getId());
        log.setAction("POSTED");
        log.setDebitAccount(debitCode);
        log.setCreditAccount(creditCode);
        log.setAmount(saved.getAmount());
        log.setSourceModule(sourceModule != null ? sourceModule : "UNKNOWN");
        log.setReferenceType(referenceType);
        log.setReferenceId(referenceId);
        log.setPerformedBy(t.getRecordedBy());
        auditLogRepository.save(log);

        auditService.log("FINANCE_POSTED", "FINANCE", "FinanceTransaction", saved.getId(),
                null,
                "amount=" + saved.getAmount()
                        + ",debit=" + debitCode
                        + ",credit=" + creditCode
                        + ",source=" + (sourceModule != null ? sourceModule : "UNKNOWN")
                        + ",refType=" + (referenceType != null ? referenceType : "")
                        + ",refId=" + (referenceId != null ? referenceId : ""));

        return saved;
    }

    public Optional<FinanceTransaction> findExistingPosting(String sourceModule, String referenceType, Long referenceId) {
        if (sourceModule == null || referenceType == null || referenceId == null) {
            return Optional.empty();
        }
        return transactionRepository.findBySourceModuleAndReferenceTypeAndReferenceId(
                sourceModule, referenceType, referenceId);
    }

    private void updateAccountBalance(ChartOfAccount coa, BigDecimal amount, boolean isDebit) {
        BigDecimal currentBalance = coa.getBalance() != null ? coa.getBalance() : BigDecimal.ZERO;
        AccountType type = coa.getAccountType();
        boolean normalDebit = (type == AccountType.ASSET || type == AccountType.EXPENSE);

        if (isDebit) {
            if (normalDebit) coa.setBalance(currentBalance.add(amount));
            else coa.setBalance(currentBalance.subtract(amount));
        } else {
            if (normalDebit) coa.setBalance(currentBalance.subtract(amount));
            else coa.setBalance(currentBalance.add(amount));
        }
        chartOfAccountRepository.save(coa);
    }

    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new HashMap<>();

        BigDecimal totalRevenue = transactionRepository.sumByType(TransactionType.INCOME);
        BigDecimal totalExpenses = transactionRepository.sumByType(TransactionType.EXPENSE);
        summary.put("totalRevenue", nvl(totalRevenue));
        summary.put("totalExpenses", nvl(totalExpenses));
        summary.put("netProfit", nvl(totalRevenue).subtract(nvl(totalExpenses)));

        Map<String, BigDecimal> unitRevenue = new HashMap<>();
        Map<String, BigDecimal> unitExpenses = new HashMap<>();
        for (BusinessUnit unit : BusinessUnit.values()) {
            unitRevenue.put(unit.name(), nvl(transactionRepository.sumByBusinessUnitAndType(unit, TransactionType.INCOME)));
            unitExpenses.put(unit.name(), nvl(transactionRepository.sumByBusinessUnitAndType(unit, TransactionType.EXPENSE)));
        }
        summary.put("unitRevenue", unitRevenue);
        summary.put("unitExpenses", unitExpenses);

        List<FinanceTransaction> allTx = transactionRepository.findAll();
        summary.put("revenueByModule", allTx.stream()
                .filter(t -> t.getTransactionType() == TransactionType.INCOME)
                .collect(Collectors.groupingBy(
                        t -> t.getSourceModule() != null ? t.getSourceModule() : FinanceSourceTypes.MODULE_MANUAL,
                        Collectors.reducing(BigDecimal.ZERO, FinanceTransaction::getAmount, BigDecimal::add))));
        summary.put("expenseByModule", allTx.stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        t -> t.getSourceModule() != null ? t.getSourceModule() : FinanceSourceTypes.MODULE_MANUAL,
                        Collectors.reducing(BigDecimal.ZERO, FinanceTransaction::getAmount, BigDecimal::add))));

        // AR from authoritative open invoices (not cancelled)
        BigDecimal invoiceAr = invoiceRepository.sumOutstandingOpen();
        summary.put("accountsReceivable", nvl(invoiceAr));
        summary.put("accountsReceivableLedger", chartOfAccountRepository.findByAccountCode("1200")
                .map(ChartOfAccount::getBalance).orElse(ZERO));
        summary.put("accountsPayable", chartOfAccountRepository.findByAccountCode(AccountCode.LIABILITY_AP.getCode())
                .map(ChartOfAccount::getBalance).orElse(ZERO));
        summary.put("cashInHand", chartOfAccountRepository.findByAccountCode("1000")
                .map(ChartOfAccount::getBalance).orElse(ZERO));
        summary.put("bankBalance", chartOfAccountRepository.findByAccountCode("1010")
                .map(ChartOfAccount::getBalance).orElse(ZERO));
        summary.put("inventoryAsset", chartOfAccountRepository.findByAccountCode("1400")
                .map(ChartOfAccount::getBalance).orElse(ZERO));

        // Cash movement from ledger (cash/bank debit - credit) — not unpaid invoices
        summary.put("cashInflow", nvl(transactionRepository.sumDebitsToAccount("1000"))
                .add(nvl(transactionRepository.sumDebitsToAccount("1010"))));
        summary.put("cashOutflow", nvl(transactionRepository.sumCreditsFromAccount("1000"))
                .add(nvl(transactionRepository.sumCreditsFromAccount("1010"))));

        summary.put("openInvoiceCount", invoiceRepository.countOpen());
        summary.put("outstandingInvoiceTotal", nvl(invoiceAr));
        summary.put("accountingNote",
                "Revenue is recognized on invoice/sale posting (INCOME). Cash receipts settle AR via TRANSFER and are not additional revenue.");

        return summary;
    }

    public BigDecimal getCashOnHandBalance() {
        return chartOfAccountRepository.findByAccountCode("1000")
                .map(ChartOfAccount::getBalance).orElse(ZERO);
    }

    public List<FinanceTransaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public List<ChartOfAccount> getAllAccounts() {
        return chartOfAccountRepository.findAll();
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }
}
