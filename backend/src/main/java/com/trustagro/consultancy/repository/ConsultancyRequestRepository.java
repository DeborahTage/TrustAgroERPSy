package com.trustagro.consultancy.repository;

import com.trustagro.consultancy.entity.ConsultancyRequest;
import com.trustagro.consultancy.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultancyRequestRepository extends JpaRepository<ConsultancyRequest, Long> {
    Optional<ConsultancyRequest> findByRequestNumber(String requestNumber);
    List<ConsultancyRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<ConsultancyRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);
    long countByStatus(RequestStatus status);

    Optional<ConsultancyRequest> findByCrmQuotationId(Long crmQuotationId);
}
