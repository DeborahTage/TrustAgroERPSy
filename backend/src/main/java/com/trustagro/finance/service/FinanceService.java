package com.trustagro.finance.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.FinancePostingConflictException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.finance.entity.*;
import com.trustagro.finance.repository.ChartOfAccountRepository;
import com.trustagro.finance.repository.FinanceAuditLogRepository;
import com.trustagro.finance.repository.FinanceTransactionRepository;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
        return postTransactions(List.of(new FinancePostingRequest(type, amount, unit,
                debitAccountCode, creditAccountCode, description, sourceModule, referenceType,
                referenceId, paymentMethod, transactionDate))).get(0);
    }

    /**
     * Locks every account needed by the operation in one account-ID-ordered query before
     * applying any posting. Multi-entry callers must use this method to avoid retaining
     * a partial account-lock set while requesting later locks in a different order.
     */
    @Transactional
    public List<FinanceTransaction> postTransactions(List<FinancePostingRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BusinessException("At least one finance posting is required");
        }

        List<PreparedPosting> postings = requests.stream()
                .map(this::preparePosting)
                .toList();
        Map<ReferenceIdentity, PreparedPosting> referencesInBatch = new HashMap<>();
        for (PreparedPosting posting : postings) {
            if (posting.identity == null) {
                continue;
            }
            PreparedPosting previous = referencesInBatch.putIfAbsent(posting.identity, posting);
            if (previous != null) {
                if (!previous.hasSameFingerprint(posting)) {
                    throw conflict(posting.identity);
                }
                throw new BusinessException("A finance posting batch cannot contain the same reference more than once");
            }
            findExisting(posting);
        }

        LinkedHashSet<String> accountCodes = new LinkedHashSet<>();
        for (PreparedPosting posting : postings) {
            if (posting.existing == null) {
                accountCodes.add(posting.debitAccountCode);
                accountCodes.add(posting.creditAccountCode);
            }
        }

        Map<String, ChartOfAccount> accountsByCode = new HashMap<>();
        if (!accountCodes.isEmpty()) {
            List<ChartOfAccount> lockedAccounts = chartOfAccountRepository
                    .findByAccountCodeInForUpdate(new ArrayList<>(accountCodes));
            accountsByCode.putAll(lockedAccounts.stream().collect(Collectors.toMap(
                    account -> normalize(account.getAccountCode()), account -> account)));
            for (PreparedPosting posting : postings) {
                if (posting.existing == null) {
                    requireAccount(accountsByCode, posting.debitAccountCode, "Debit");
                    requireAccount(accountsByCode, posting.creditAccountCode, "Credit");
                }
            }
        }

        // Recheck after the account locks: identical concurrent requests using the same
        // accounts serialize here and observe the committed posting before changing balances.
        for (PreparedPosting posting : postings) {
            if (posting.existing == null) {
                findExisting(posting);
            }
        }

        for (PreparedPosting posting : postings) {
            if (posting.existing == null) {
                posting.existing = applyPosting(posting,
                        accountsByCode.get(posting.debitAccountCode),
                        accountsByCode.get(posting.creditAccountCode));
            }
        }
        return postings.stream().map(posting -> posting.existing).toList();
    }

    public Optional<FinanceTransaction> findExistingPosting(String sourceModule, String referenceType, Long referenceId) {
        if (sourceModule == null || referenceType == null || referenceId == null) {
            return Optional.empty();
        }
        return transactionRepository.findBySourceModuleAndReferenceTypeAndReferenceId(
                normalize(sourceModule), normalize(referenceType), referenceId);
    }

    private PreparedPosting preparePosting(FinancePostingRequest request) {
        if (request == null) {
            throw new BusinessException("Finance posting cannot be null");
        }
        BigDecimal amount = request.amount();
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
        if (request.transactionType() == null) {
            throw new BusinessException("Finance transaction type is required");
        }

        String debitCode = normalize(request.debitAccountCode());
        String creditCode = normalize(request.creditAccountCode());
        if (debitCode == null || debitCode.isBlank()) {
            throw new BusinessException("Debit account is required");
        }
        if (creditCode == null || creditCode.isBlank()) {
            throw new BusinessException("Credit account is required");
        }
        if (debitCode.equals(creditCode)) {
            throw new BusinessException("Debit and credit accounts must differ");
        }

        String sourceModule = normalize(request.sourceModule());
        String referenceType = normalize(request.referenceType());
        ReferenceIdentity identity = sourceModule != null && referenceType != null && request.referenceId() != null
                ? new ReferenceIdentity(sourceModule, referenceType, request.referenceId()) : null;
        return new PreparedPosting(request, postedAmount, debitCode, creditCode,
                sourceModule, referenceType, identity);
    }

    private void findExisting(PreparedPosting posting) {
        if (posting.identity == null) {
            return;
        }
        transactionRepository.findBySourceModuleAndReferenceTypeAndReferenceId(
                        posting.identity.sourceModule, posting.identity.referenceType, posting.identity.referenceId)
                .ifPresent(existing -> {
                    if (!posting.matches(existing)) {
                        throw conflict(posting.identity);
                    }
                    posting.existing = existing;
                });
    }

    private ChartOfAccount requireAccount(Map<String, ChartOfAccount> accountsByCode,
                                          String accountCode, String label) {
        ChartOfAccount account = accountsByCode.get(accountCode);
        if (account == null) {
            throw new ResourceNotFoundException(label + " account not found: " + accountCode);
        }
        return account;
    }

    private FinanceTransaction applyPosting(PreparedPosting posting,
                                            ChartOfAccount debitCoa, ChartOfAccount creditCoa) {
        FinancePostingRequest request = posting.request;
        FinanceTransaction transaction = new FinanceTransaction();
        transaction.setTransactionType(request.transactionType());
        transaction.setAmount(posting.amount);
        transaction.setBusinessUnit(request.businessUnit() != null ? request.businessUnit() : BusinessUnit.GENERAL);
        transaction.setDescription(request.description());
        transaction.setSourceModule(posting.sourceModule);
        transaction.setReferenceType(posting.referenceType);
        transaction.setReferenceId(request.referenceId());
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setTransactionDate(request.transactionDate() != null ? request.transactionDate() : LocalDate.now());
        transaction.setCategory(posting.referenceType);

        String email = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        userRepository.findByEmail(email).ifPresent(transaction::setRecordedBy);

        transaction.setDebitAccount(debitCoa);
        transaction.setCreditAccount(creditCoa);
        updateAccountBalance(debitCoa, posting.amount, true);
        updateAccountBalance(creditCoa, posting.amount, false);

        FinanceTransaction saved;
        try {
            saved = transactionRepository.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException exception) {
            if (isSourceReferenceConstraintViolation(exception) && posting.identity != null) {
                throw conflict(posting.identity);
            }
            throw exception;
        }

        FinanceAuditLog log = new FinanceAuditLog();
        log.setTransactionId(saved.getId());
        log.setAction("POSTED");
        log.setDebitAccount(posting.debitAccountCode);
        log.setCreditAccount(posting.creditAccountCode);
        log.setAmount(saved.getAmount());
        log.setSourceModule(posting.sourceModule != null ? posting.sourceModule : "UNKNOWN");
        log.setReferenceType(posting.referenceType);
        log.setReferenceId(request.referenceId());
        log.setPerformedBy(transaction.getRecordedBy());
        auditLogRepository.save(log);

        auditService.log("FINANCE_POSTED", "FINANCE", "FinanceTransaction", saved.getId(),
                null,
                "amount=" + saved.getAmount()
                        + ",debit=" + posting.debitAccountCode
                        + ",credit=" + posting.creditAccountCode
                        + ",source=" + (posting.sourceModule != null ? posting.sourceModule : "UNKNOWN")
                        + ",refType=" + (posting.referenceType != null ? posting.referenceType : "")
                        + ",refId=" + (request.referenceId() != null ? request.referenceId() : ""));
        return saved;
    }

    private boolean isSourceReferenceConstraintViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && "uk_finance_tx_source_ref".equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    private FinancePostingConflictException conflict(ReferenceIdentity identity) {
        return new FinancePostingConflictException("Finance posting reference "
                + identity.sourceModule + "/" + identity.referenceType + "/" + identity.referenceId
                + " already exists with different posting details");
    }

    private static String normalize(String value) {
        return value != null ? value.trim() : null;
    }

    private record ReferenceIdentity(String sourceModule, String referenceType, Long referenceId) {
    }

    private static final class PreparedPosting {
        private final FinancePostingRequest request;
        private final BigDecimal amount;
        private final String debitAccountCode;
        private final String creditAccountCode;
        private final String sourceModule;
        private final String referenceType;
        private final ReferenceIdentity identity;
        private FinanceTransaction existing;

        private PreparedPosting(FinancePostingRequest request, BigDecimal amount,
                                String debitAccountCode, String creditAccountCode,
                                String sourceModule, String referenceType,
                                ReferenceIdentity identity) {
            this.request = request;
            this.amount = amount;
            this.debitAccountCode = debitAccountCode;
            this.creditAccountCode = creditAccountCode;
            this.sourceModule = sourceModule;
            this.referenceType = referenceType;
            this.identity = identity;
        }

        private boolean hasSameFingerprint(PreparedPosting other) {
            return Objects.equals(identity, other.identity)
                    && request.transactionType() == other.request.transactionType()
                    && amount.compareTo(other.amount) == 0
                    && debitAccountCode.equals(other.debitAccountCode)
                    && creditAccountCode.equals(other.creditAccountCode);
        }

        private boolean matches(FinanceTransaction transaction) {
            return request.transactionType() == transaction.getTransactionType()
                    && transaction.getAmount() != null && amount.compareTo(transaction.getAmount()) == 0
                    && transaction.getDebitAccount() != null
                    && debitAccountCode.equals(normalize(transaction.getDebitAccount().getAccountCode()))
                    && transaction.getCreditAccount() != null
                    && creditAccountCode.equals(normalize(transaction.getCreditAccount().getAccountCode()));
        }
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
        return transactionRepository.findAllWithAccounts();
    }

    public List<ChartOfAccount> getAllAccounts() {
        return chartOfAccountRepository.findAll();
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }
}
