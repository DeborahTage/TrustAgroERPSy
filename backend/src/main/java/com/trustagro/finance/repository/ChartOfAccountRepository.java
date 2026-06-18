package com.trustagro.finance.repository;

import com.trustagro.finance.entity.AccountType;
import com.trustagro.finance.entity.ChartOfAccount;
import com.trustagro.finance.entity.BusinessUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long> {
    Optional<ChartOfAccount> findByAccountCode(String accountCode);
    List<ChartOfAccount> findByBusinessUnit(BusinessUnit businessUnit);
    List<ChartOfAccount> findByAccountType(AccountType type);
}

