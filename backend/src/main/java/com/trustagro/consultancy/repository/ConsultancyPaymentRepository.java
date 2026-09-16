package com.trustagro.consultancy.repository;

import com.trustagro.consultancy.entity.ConsultancyPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultancyPaymentRepository extends JpaRepository<ConsultancyPayment, Long> {
    Optional<ConsultancyPayment> findByPaymentNumber(String paymentNumber);
    List<ConsultancyPayment> findByEngagementIdOrderByPaymentDateAsc(Long engagementId);
}
