package com.trustagro.finance.repository;

import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, Long> {

    List<FinanceTransaction> findByTransactionType(TransactionType type);

    List<FinanceTransaction> findByTransactionDateBetween(LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinanceTransaction t WHERE t.transactionType = :type")
    BigDecimal sumByType(@Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinanceTransaction t WHERE t.transactionType = :type AND t.transactionDate BETWEEN :from AND :to")
    BigDecimal sumByTypeAndDateRange(@Param("type") TransactionType type, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinanceTransaction t WHERE t.businessUnit = :unit AND t.transactionType = :type")
    BigDecimal sumByBusinessUnitAndType(@Param("unit") com.trustagro.finance.entity.BusinessUnit unit, @Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.debitAccount.accountCode = :code THEN t.amount ELSE 0 END), 0) - " +
           "COALESCE(SUM(CASE WHEN t.creditAccount.accountCode = :code THEN t.amount ELSE 0 END), 0) " +
           "FROM FinanceTransaction t WHERE (t.debitAccount.accountCode = :code OR t.creditAccount.accountCode = :code) " +
           "AND t.transactionDate BETWEEN :from AND :to")
    BigDecimal sumByAccountInRange(@Param("code") String code, @Param("from") LocalDate from, @Param("to") LocalDate to);
}

