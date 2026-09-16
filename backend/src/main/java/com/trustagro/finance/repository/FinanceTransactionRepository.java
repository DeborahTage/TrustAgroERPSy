package com.trustagro.finance.repository;

import com.trustagro.finance.entity.FinanceTransaction;
import com.trustagro.finance.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, Long> {

    List<FinanceTransaction> findByTransactionType(TransactionType type);

    List<FinanceTransaction> findByTransactionDateBetween(LocalDate from, LocalDate to);

    @Query("SELECT DISTINCT t FROM FinanceTransaction t "
            + "LEFT JOIN FETCH t.debitAccount LEFT JOIN FETCH t.creditAccount "
            + "WHERE t.transactionDate BETWEEN :from AND :to")
    List<FinanceTransaction> findByTransactionDateBetweenWithAccounts(
            @Param("from") LocalDate from, @Param("to") LocalDate to);

    Optional<FinanceTransaction> findBySourceModuleAndReferenceTypeAndReferenceId(
            String sourceModule, String referenceType, Long referenceId);

    boolean existsBySourceModuleAndReferenceTypeAndReferenceId(
            String sourceModule, String referenceType, Long referenceId);

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

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinanceTransaction t WHERE t.debitAccount.accountCode = :code")
    BigDecimal sumDebitsToAccount(@Param("code") String code);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinanceTransaction t WHERE t.creditAccount.accountCode = :code")
    BigDecimal sumCreditsFromAccount(@Param("code") String code);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinanceTransaction t "
            + "WHERE t.transactionType = :type AND t.transactionDate BETWEEN :from AND :to "
            + "AND (:module IS NULL OR t.sourceModule = :module)")
    BigDecimal sumByTypeDateRangeAndModule(@Param("type") TransactionType type,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to,
                                           @Param("module") String module);

    @Query("SELECT COALESCE(t.sourceModule, 'MANUAL'), COALESCE(SUM(t.amount), 0) FROM FinanceTransaction t "
            + "WHERE t.transactionType = :type AND t.transactionDate BETWEEN :from AND :to "
            + "GROUP BY t.sourceModule")
    List<Object[]> sumGroupedByModule(@Param("type") TransactionType type,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinanceTransaction t "
            + "WHERE t.debitAccount.accountCode = :code AND t.transactionDate BETWEEN :from AND :to")
    BigDecimal sumDebitsToAccountInRange(@Param("code") String code,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinanceTransaction t "
            + "WHERE t.creditAccount.accountCode = :code AND t.transactionDate BETWEEN :from AND :to")
    BigDecimal sumCreditsFromAccountInRange(@Param("code") String code,
                                            @Param("from") LocalDate from,
                                            @Param("to") LocalDate to);

    /**
     * Monthly trend buckets. Prefer portable month iteration in callers if the dialect
     * rejects {@code FUNCTION('YEAR'|MONTH')} — H2 local profile uses MODE=PostgreSQL.
     */
    @Query("SELECT FUNCTION('YEAR', t.transactionDate), FUNCTION('MONTH', t.transactionDate), COALESCE(SUM(t.amount), 0) "
            + "FROM FinanceTransaction t WHERE t.transactionType = :type AND t.transactionDate BETWEEN :from AND :to "
            + "GROUP BY FUNCTION('YEAR', t.transactionDate), FUNCTION('MONTH', t.transactionDate) ORDER BY 1, 2")
    List<Object[]> sumByTypeGroupedByYearMonth(@Param("type") TransactionType type,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);
}
