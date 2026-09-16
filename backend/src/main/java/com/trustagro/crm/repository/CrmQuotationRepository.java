package com.trustagro.crm.repository;

import com.trustagro.crm.entity.CrmQuotation;
import com.trustagro.crm.entity.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CrmQuotationRepository extends JpaRepository<CrmQuotation, Long> {
    List<CrmQuotation> findByClientIdOrderByCreatedAtDesc(Long clientId);
    List<CrmQuotation> findByStatusOrderByCreatedAtDesc(QuotationStatus status);
    List<CrmQuotation> findAllByOrderByCreatedAtDesc();
    Optional<CrmQuotation> findByConvertedOrderId(Long orderId);

    long countByStatus(QuotationStatus status);

    @Query("SELECT q.status, COUNT(q) FROM CrmQuotation q GROUP BY q.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT COUNT(q) FROM CrmQuotation q WHERE q.status = com.trustagro.crm.entity.QuotationStatus.ACCEPTED " +
           "AND q.convertedOrderId IS NULL AND q.consultancyRequestId IS NULL")
    long countAcceptedWithoutConversion();
}
