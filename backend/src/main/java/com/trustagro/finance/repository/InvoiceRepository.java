package com.trustagro.finance.repository;

import com.trustagro.finance.entity.Invoice;
import com.trustagro.finance.entity.InvoiceStatus;
import com.trustagro.finance.entity.InvoiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
