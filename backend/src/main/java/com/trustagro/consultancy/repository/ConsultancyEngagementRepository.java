package com.trustagro.consultancy.repository;

import com.trustagro.consultancy.entity.ConsultancyEngagement;
import com.trustagro.consultancy.entity.EngagementStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConsultancyEngagementRepository extends JpaRepository<ConsultancyEngagement, Long> {
    Optional<ConsultancyEngagement> findByEngagementNumber(String engagementNumber);
    List<ConsultancyEngagement> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<ConsultancyEngagement> findByStatusOrderByCreatedAtDesc(EngagementStatus status);
    long countByStatus(EngagementStatus status);
    List<ConsultancyEngagement> findByLeadConsultantIdOrderByCreatedAtDesc(Long leadConsultantId);

    Optional<ConsultancyEngagement> findByRequestId(Long requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM ConsultancyEngagement e WHERE e.id = :id")
    Optional<ConsultancyEngagement> findByIdForUpdate(@Param("id") Long id);
}
