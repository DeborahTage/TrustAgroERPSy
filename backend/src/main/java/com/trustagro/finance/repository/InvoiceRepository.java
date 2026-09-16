package com.trustagro.finance.repository;

import com.trustagro.finance.entity.Invoice;
import com.trustagro.finance.entity.InvoiceStatus;
import com.trustagro.finance.entity.InvoiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    List<Invoice> findByStatus(InvoiceStatus status);
    List<Invoice> findByClientId(Long clientId);
    List<Invoice> findByInvoiceType(InvoiceType type);
    List<Invoice> findByDueDateBeforeAndStatusNot(LocalDate date, InvoiceStatus status);
    Optional<Invoice> findBySourceModuleAndSourceId(String sourceModule, Long sourceId);
    Optional<Invoice> findBySourceModuleAndSourceTypeAndSourceId(String sourceModule, String sourceType, Long sourceId);

    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) FROM Invoice i " +
           "WHERE i.status IN (com.trustagro.finance.entity.InvoiceStatus.SENT, " +
           "com.trustagro.finance.entity.InvoiceStatus.PARTIALLY_PAID, " +
           "com.trustagro.finance.entity.InvoiceStatus.OVERDUE)")
    java.math.BigDecimal sumOutstandingOpen();

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.status IN (" +
           "com.trustagro.finance.entity.InvoiceStatus.SENT, " +
           "com.trustagro.finance.entity.InvoiceStatus.PARTIALLY_PAID, " +
           "com.trustagro.finance.entity.InvoiceStatus.OVERDUE)")
    long countOpen();

    long countBySourceModule(String sourceModule);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.sourceModule = :sourceModule")
    java.math.BigDecimal sumTotalAmountBySourceModule(@Param("sourceModule") String sourceModule);

    @Query("SELECT COALESCE(SUM(i.paidAmount), 0) FROM Invoice i WHERE i.sourceModule = :sourceModule")
    java.math.BigDecimal sumPaidAmountBySourceModule(@Param("sourceModule") String sourceModule);

    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) FROM Invoice i " +
           "WHERE i.sourceModule = :sourceModule AND i.status IN (" +
           "com.trustagro.finance.entity.InvoiceStatus.SENT, " +
           "com.trustagro.finance.entity.InvoiceStatus.PARTIALLY_PAID, " +
           "com.trustagro.finance.entity.InvoiceStatus.OVERDUE)")
    java.math.BigDecimal sumOutstandingBySourceModule(@Param("sourceModule") String sourceModule);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.dueDate < :today AND i.status IN (" +
           "com.trustagro.finance.entity.InvoiceStatus.SENT, " +
           "com.trustagro.finance.entity.InvoiceStatus.PARTIALLY_PAID, " +
           "com.trustagro.finance.entity.InvoiceStatus.OVERDUE)")
    long countOverdueOpen(@Param("today") LocalDate today);

    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) FROM Invoice i " +
           "WHERE i.dueDate < :today AND i.status IN (" +
           "com.trustagro.finance.entity.InvoiceStatus.SENT, " +
           "com.trustagro.finance.entity.InvoiceStatus.PARTIALLY_PAID, " +
           "com.trustagro.finance.entity.InvoiceStatus.OVERDUE)")
    java.math.BigDecimal sumOverdueOutstanding(@Param("today") LocalDate today);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.status IN (" +
           "com.trustagro.finance.entity.InvoiceStatus.SENT, " +
           "com.trustagro.finance.entity.InvoiceStatus.PARTIALLY_PAID, " +
           "com.trustagro.finance.entity.InvoiceStatus.OVERDUE) " +
           "AND (i.sourceModule IS NULL OR i.sourceModule = '' OR i.sourceId IS NULL)")
    long countOpenWithoutSource();
}
