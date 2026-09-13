package com.trustagro.finance.service;

import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.finance.entity.CustomerWallet;
import com.trustagro.finance.entity.WalletTransaction;
import com.trustagro.finance.repository.CustomerWalletRepository;
import com.trustagro.finance.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final CustomerWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    public List<CustomerWallet> getAllWallets() {
        return walletRepository.findAll();
    }

    public Map<String, Object> getWalletSummary() {
        List<CustomerWallet> wallets = walletRepository.findAll();
        BigDecimal totalDeposits = wallets.stream()
                .map(CustomerWallet::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExposure = wallets.stream()
                .map(CustomerWallet::getCreditLimit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDeposits", totalDeposits);
        summary.put("activeWallets", wallets.size());
        summary.put("totalExposure", totalExposure);
        return summary;
    }

    @Transactional
    public CustomerWallet deposit(Long clientId, String clientName, BigDecimal amount) {
        CustomerWallet wallet = walletRepository.findByClientId(clientId)
                .orElseGet(() -> {
                    CustomerWallet newWallet = new CustomerWallet();
                    newWallet.setClientId(clientId);
                    newWallet.setClientName(clientName);
                    return newWallet;
                });
        
        wallet.setBalance(wallet.getBalance().add(amount));
        CustomerWallet saved = walletRepository.save(wallet);
        
        logTransaction(saved, amount, "DEPOSIT", "Wallet deposit");
        return saved;
    }

    @Transactional
    public boolean deduct(Long clientId, BigDecimal amount, String description, String refType, Long refId) {
        CustomerWallet wallet = walletRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for client: " + clientId));
        
        if (wallet.getBalance().add(wallet.getCreditLimit()).compareTo(amount) < 0) {
            return false;
        }
        
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
        
        logTransaction(wallet, amount, "DEDUCTION", description, refType, refId);
        return true;
    }

    private void logTransaction(CustomerWallet wallet, BigDecimal amount, String type, String description) {
        logTransaction(wallet, amount, type, description, null, null);
    }

    private void logTransaction(CustomerWallet wallet, BigDecimal amount, String type, String description, String refType, Long refId) {
        WalletTransaction t = new WalletTransaction();
        t.setWallet(wallet);
        t.setAmount(amount);
        t.setType(type);
        t.setDescription(description);
        t.setReferenceType(refType);
        t.setReferenceId(refId);
        transactionRepository.save(t);
    }
}
