package com.trustagro.consultancy.repository;

import com.trustagro.consultancy.entity.ConsultancyContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ConsultancyContractRepository extends JpaRepository<ConsultancyContract, Long> {
    Optional<ConsultancyContract> findByContractNumber(String contractNumber);
    Optional<ConsultancyContract> findByEngagementId(Long engagementId);
    boolean existsByEngagementId(Long engagementId);

    @Query("SELECT COUNT(c) FROM ConsultancyContract c WHERE c.engagement.request.crmQuotationId IS NOT NULL")
    long countByEngagementRequestCrmQuotationIdIsNotNull();
}
