package com.trustagro.finance.service;

import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.finance.entity.*;
import com.trustagro.finance.repository.ChartOfAccountRepository;
import com.trustagro.finance.repository.FinanceAuditLogRepository;
import com.trustagro.finance.repository.FinanceTransactionRepository;

import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final FinanceTransactionRepository transactionRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final FinanceAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;


    @Transactional
    public FinanceTransaction postRevenue(BigDecimal amount, BusinessUnit unit, String accountCode,
                                         String description, String sourceModule, String referenceType, Long referenceId) {
        // Double entry: Debit Cash (1000), Credit Revenue Account
        return postTransaction(TransactionType.INCOME, amount, unit, "1000", accountCode, 
                description, sourceModule, referenceType, referenceId);
    }

    @Transactional
    public FinanceTransaction postExpense(BigDecimal amount, BusinessUnit unit, String accountCode,
                                         String description, String sourceModule, String referenceType, Long referenceId) {
        // Double entry: Debit Expense Account, Credit Cash (1000)
        return postTransaction(TransactionType.EXPENSE, amount, unit, accountCode, "1000",
                description, sourceModule, referenceType, referenceId);
    }


    @Transactional
    public FinanceTransaction postTransaction(TransactionType type, BigDecimal amount, BusinessUnit unit,
                                             String debitAccountCode, String creditAccountCode,
                                             String description, String sourceModule,
                                             String referenceType, Long referenceId) {
        
        FinanceTransaction t = new FinanceTransaction();
        t.setTransactionType(type);
        t.setAmount(amount);
        t.setBusinessUnit(unit != null ? unit : BusinessUnit.GENERAL);
        t.setDescription(description);
        t.setSourceModule(sourceModule);
        t.setTransactionDate(LocalDate.now());

        String email = SecurityContextHolder.getContext().getAuthentication() != null 
                ? SecurityContextHolder.getContext().getAuthentication().getName() : "system";
        userRepository.findByEmail(email).ifPresent(t::setRecordedBy);

        // Map Debit Account
        if (debitAccountCode != null) {
            ChartOfAccount debitCoa = chartOfAccountRepository.findByAccountCode(debitAccountCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Debit account not found: " + debitAccountCode));
            t.setDebitAccount(debitCoa);
            updateAccountBalance(debitCoa, amount, true); // Debit increases Assets/Expenses, decreases Liabilities/Revenue/Equity
        }

        // Map Credit Account
        if (creditAccountCode != null) {
            ChartOfAccount creditCoa = chartOfAccountRepository.findByAccountCode(creditAccountCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Credit account not found: " + creditAccountCode));
            t.setCreditAccount(creditCoa);
            updateAccountBalance(creditCoa, amount, false); // Credit decreases Assets/Expenses, increases Liabilities/Revenue/Equity
        }

        FinanceTransaction saved = transactionRepository.save(t);

        // Write immutable audit record
        FinanceAuditLog log = new FinanceAuditLog();
        log.setTransactionId(saved.getId());
        log.setAction("POSTED");
        log.setDebitAccount(debitAccountCode != null ? debitAccountCode : "");
        log.setCreditAccount(creditAccountCode != null ? creditAccountCode : "");
        log.setAmount(amount);
        log.setSourceModule(sourceModule != null ? sourceModule : "UNKNOWN");
        log.setReferenceType(referenceType);
        log.setReferenceId(referenceId);
        log.setPerformedBy(t.getRecordedBy());
        auditLogRepository.save(log);

        return saved;
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
        
        summary.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        summary.put("totalExpenses", totalExpenses != null ? totalExpenses : BigDecimal.ZERO);
        summary.put("netProfit", (totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .subtract(totalExpenses != null ? totalExpenses : BigDecimal.ZERO));
        
        // Per-unit breakdown
        Map<String, BigDecimal> unitRevenue = new HashMap<>();
        for (BusinessUnit unit : BusinessUnit.values()) {
            BigDecimal val = transactionRepository.sumByBusinessUnitAndType(unit, TransactionType.INCOME);
            unitRevenue.put(unit.name(), val != null ? val : BigDecimal.ZERO);
        }
        summary.put("unitRevenue", unitRevenue);

        // Real Metrics required for Dashboard
        summary.put("accountsReceivable", chartOfAccountRepository.findByAccountCode("1200")
                .map(ChartOfAccount::getBalance).orElse(BigDecimal.ZERO));
        summary.put("accountsPayable", chartOfAccountRepository.findByAccountCode("2200")
                .map(ChartOfAccount::getBalance).orElse(BigDecimal.ZERO));
        summary.put("cashInHand", chartOfAccountRepository.findByAccountCode("1000")
                .map(ChartOfAccount::getBalance).orElse(BigDecimal.ZERO));
        
        return summary;
    }


    public List<FinanceTransaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public List<ChartOfAccount> getAllAccounts() {
        return chartOfAccountRepository.findAll();
    }
}
