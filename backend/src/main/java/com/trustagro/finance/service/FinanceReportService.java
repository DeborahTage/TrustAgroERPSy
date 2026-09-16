package com.trustagro.finance.service;

import com.trustagro.finance.entity.AccountType;
import com.trustagro.finance.entity.ChartOfAccount;
import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.repository.ChartOfAccountRepository;
import com.trustagro.finance.repository.FinanceTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceReportService {

    private final FinanceTransactionRepository transactionRepository;
    private final ChartOfAccountRepository coaRepository;

    public Map<String, Object> getProfitAndLoss(LocalDate from, LocalDate to) {
        Map<String, Object> report = new HashMap<>();
        
        List<ChartOfAccount> revenueAccounts = coaRepository.findByAccountType(AccountType.REVENUE);
        List<ChartOfAccount> expenseAccounts = coaRepository.findByAccountType(AccountType.EXPENSE);

        Map<String, BigDecimal> revenues = new HashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (ChartOfAccount acc : revenueAccounts) {
            BigDecimal balance = transactionRepository.sumByAccountInRange(acc.getAccountCode(), from, to);
            // In P&L, Revenue is normally credit, so we show it as positive
            balance = balance != null ? balance.abs() : BigDecimal.ZERO;
            // Skip empty Training/Consulting lines so P&L does not imply those modules exist
            if (balance.compareTo(BigDecimal.ZERO) == 0
                    && ("Training Revenue".equals(acc.getAccountName())
                    || "Workshop Revenue".equals(acc.getAccountName())
                    || "Consulting Revenue".equals(acc.getAccountName())
                    || "Training Costs".equals(acc.getAccountName())
                    || "Consulting Costs".equals(acc.getAccountName()))) {
                continue;
            }
            revenues.put(acc.getAccountName(), balance);
            totalRevenue = totalRevenue.add(balance);
        }

        Map<String, BigDecimal> expenses = new HashMap<>();
        BigDecimal totalExpenses = BigDecimal.ZERO;
        BigDecimal knownCogs = BigDecimal.ZERO;
        for (ChartOfAccount acc : expenseAccounts) {
            BigDecimal balance = transactionRepository.sumByAccountInRange(acc.getAccountCode(), from, to);
            balance = balance != null ? balance.abs() : BigDecimal.ZERO;
            if (balance.compareTo(BigDecimal.ZERO) == 0
                    && ("Training Costs".equals(acc.getAccountName())
                    || "Consulting Costs".equals(acc.getAccountName()))) {
                continue;
            }
            expenses.put(acc.getAccountName(), balance);
            totalExpenses = totalExpenses.add(balance);
            if (acc.getAccountName() != null && acc.getAccountName().startsWith("Cost of Goods Sold")) {
                knownCogs = knownCogs.add(balance);
            }
        }

        report.put("period", from.toString() + " to " + to.toString());
        report.put("revenues", revenues);
        report.put("totalRevenue", totalRevenue);
        report.put("expenses", expenses);
        report.put("totalExpenses", totalExpenses);
        report.put("knownCogs", knownCogs);
        report.put("netProfit", totalRevenue.subtract(totalExpenses));
        report.put("limitationNote",
                "P&L uses posted ledger activity only. Missing COGS/allocations are not treated as zero cost. "
                        + "Training/Consultancy lines appear only when amounts were actually posted.");

        // Module traceability for drill-down
        List<FinanceTransaction> periodTx = transactionRepository.findByTransactionDateBetween(from, to);
        report.put("revenueByModule", periodTx.stream()
                .filter(t -> t.getTransactionType() == TransactionType.INCOME)
                .collect(Collectors.groupingBy(
                        t -> t.getSourceModule() != null ? t.getSourceModule() : "MANUAL",
                        Collectors.reducing(BigDecimal.ZERO, FinanceTransaction::getAmount, BigDecimal::add))));
        report.put("expenseByModule", periodTx.stream()
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        t -> t.getSourceModule() != null ? t.getSourceModule() : "MANUAL",
                        Collectors.reducing(BigDecimal.ZERO, FinanceTransaction::getAmount, BigDecimal::add))));

        return report;
    }

    /**
     * Cash movement from cash/bank accounts only — unpaid invoices are excluded.
     */
    public Map<String, Object> getCashFlow(LocalDate from, LocalDate to) {
        Map<String, Object> report = new HashMap<>();
        BigDecimal cashIn = BigDecimal.ZERO;
        BigDecimal cashOut = BigDecimal.ZERO;
        List<FinanceTransaction> periodTx = transactionRepository.findByTransactionDateBetweenWithAccounts(from, to);
        for (FinanceTransaction t : periodTx) {
            String debit = t.getDebitAccount() != null ? t.getDebitAccount().getAccountCode() : null;
            String credit = t.getCreditAccount() != null ? t.getCreditAccount().getAccountCode() : null;
            boolean debitCash = isCashOrBank(debit);
            boolean creditCash = isCashOrBank(credit);
            if (debitCash && !creditCash) {
                cashIn = cashIn.add(t.getAmount());
            }
            if (creditCash && !debitCash) {
                cashOut = cashOut.add(t.getAmount());
            }
        }
        report.put("period", from + " to " + to);
        report.put("cashInflows", cashIn);
        report.put("cashOutflows", cashOut);
        report.put("netCashChange", cashIn.subtract(cashOut));
        report.put("note", "Cash flow counts only movements that debit/credit cash (1000) or bank (1010). "
                + "Invoice issuance and AR increases are not cash inflows.");
        return report;
    }

    private boolean isCashOrBank(String code) {
        return "1000".equals(code) || "1010".equals(code);
    }

    public Map<String, Object> getBalanceSheet() {
        Map<String, Object> report = new HashMap<>();
        
        List<ChartOfAccount> assets = coaRepository.findByAccountType(AccountType.ASSET);
        List<ChartOfAccount> liabilities = coaRepository.findByAccountType(AccountType.LIABILITY);
        List<ChartOfAccount> equity = coaRepository.findByAccountType(AccountType.EQUITY);

        Map<String, BigDecimal> assetBalances = assets.stream()
                .collect(Collectors.toMap(ChartOfAccount::getAccountName, a -> a.getBalance() != null ? a.getBalance() : BigDecimal.ZERO));
        BigDecimal totalAssets = assetBalances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> liabilityBalances = liabilities.stream()
                .collect(Collectors.toMap(ChartOfAccount::getAccountName, l -> l.getBalance() != null ? l.getBalance() : BigDecimal.ZERO));
        BigDecimal totalLiabilities = liabilityBalances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> equityBalances = equity.stream()
                .collect(Collectors.toMap(ChartOfAccount::getAccountName, e -> e.getBalance() != null ? e.getBalance() : BigDecimal.ZERO));
        BigDecimal totalEquity = equityBalances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        report.put("assets", assetBalances);
        report.put("totalAssets", totalAssets);
        report.put("liabilities", liabilityBalances);
        report.put("totalLiabilities", totalLiabilities);
        report.put("equity", equityBalances);
        report.put("totalEquity", totalEquity);
        
        return report;
    }
}
