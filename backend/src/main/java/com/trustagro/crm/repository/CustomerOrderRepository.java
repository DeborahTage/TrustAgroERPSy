package com.trustagro.crm.repository;

import com.trustagro.crm.entity.CustomerOrder;
import com.trustagro.crm.entity.CustomerOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByClientId(Long clientId);
    List<CustomerOrder> findByClientIdOrderByCreatedAtDesc(Long clientId);
    List<CustomerOrder> findByStatus(CustomerOrderStatus status);
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
    Optional<CustomerOrder> findByQuotationId(Long quotationId);

    long countByStatus(CustomerOrderStatus status);

    @Query("SELECT o.status, COUNT(o) FROM CustomerOrder o GROUP BY o.status")
    List<Object[]> countGroupedByStatus();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CustomerOrder o WHERE o.id = :id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") Long id);
}
