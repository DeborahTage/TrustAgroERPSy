package com.trustagro.finance.repository;

import com.trustagro.finance.entity.AccountType;
import com.trustagro.finance.entity.ChartOfAccount;
import com.trustagro.finance.entity.BusinessUnit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, Long> {
    Optional<ChartOfAccount> findByAccountCode(String accountCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT account FROM ChartOfAccount account "
            + "WHERE account.accountCode IN :accountCodes ORDER BY account.id")
    List<ChartOfAccount> findByAccountCodeInForUpdate(@Param("accountCodes") List<String> accountCodes);

    List<ChartOfAccount> findByBusinessUnit(BusinessUnit businessUnit);
    List<ChartOfAccount> findByAccountType(AccountType type);
}
