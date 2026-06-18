package com.trustagro.finance.service;

import com.trustagro.finance.entity.AccountType;
import com.trustagro.finance.entity.ChartOfAccount;
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
            revenues.put(acc.getAccountName(), balance);
            totalRevenue = totalRevenue.add(balance);
        }

        Map<String, BigDecimal> expenses = new HashMap<>();
        BigDecimal totalExpenses = BigDecimal.ZERO;
        for (ChartOfAccount acc : expenseAccounts) {
            BigDecimal balance = transactionRepository.sumByAccountInRange(acc.getAccountCode(), from, to);
            balance = balance != null ? balance.abs() : BigDecimal.ZERO;
            expenses.put(acc.getAccountName(), balance);
            totalExpenses = totalExpenses.add(balance);
        }

        report.put("period", from.toString() + " to " + to.toString());
        report.put("revenues", revenues);
        report.put("totalRevenue", totalRevenue);
        report.put("expenses", expenses);
        report.put("totalExpenses", totalExpenses);
        report.put("netProfit", totalRevenue.subtract(totalExpenses));

        return report;
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
