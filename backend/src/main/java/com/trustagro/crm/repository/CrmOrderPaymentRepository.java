package com.trustagro.crm.repository;

import com.trustagro.crm.entity.CrmOrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CrmOrderPaymentRepository extends JpaRepository<CrmOrderPayment, Long> {
    List<CrmOrderPayment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<CrmOrderPayment> findByIdempotencyKey(String key);

    List<CrmOrderPayment> findByOrderIdIn(Collection<Long> orderIds);
}
