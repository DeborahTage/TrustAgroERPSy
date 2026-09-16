package com.trustagro.consultancy.repository;

import com.trustagro.consultancy.entity.ConsultancyDeliverable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultancyDeliverableRepository extends JpaRepository<ConsultancyDeliverable, Long> {
    Optional<ConsultancyDeliverable> findByDeliverableNumber(String deliverableNumber);
    List<ConsultancyDeliverable> findByEngagementId(Long engagementId);
    List<ConsultancyDeliverable> findByEngagementIdOrderByDueDateAsc(Long engagementId);
}
