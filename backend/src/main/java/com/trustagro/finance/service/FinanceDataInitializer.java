package com.trustagro.finance.service;

import com.trustagro.finance.entity.AccountCode;
import com.trustagro.finance.entity.ChartOfAccount;
import com.trustagro.finance.repository.ChartOfAccountRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Idempotent Chart of Accounts seed — creates any missing AccountCode entries without wiping balances.
 */
@Component
@RequiredArgsConstructor
public class FinanceDataInitializer {

    private final ChartOfAccountRepository coaRepository;

    @PostConstruct
    public void init() {
        for (AccountCode code : AccountCode.values()) {
            if (coaRepository.findByAccountCode(code.getCode()).isEmpty()) {
                ChartOfAccount coa = new ChartOfAccount();
                coa.setAccountCode(code.getCode());
                coa.setAccountName(code.getName());
                coa.setAccountType(code.getAccountType());
                coa.setBusinessUnit(code.getUnit());
                coa.setBalance(BigDecimal.ZERO);
                coaRepository.save(coa);
            }
        }
    }
}
