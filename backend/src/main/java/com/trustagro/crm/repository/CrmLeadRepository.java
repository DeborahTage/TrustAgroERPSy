package com.trustagro.crm.repository;

import com.trustagro.crm.entity.CrmLead;
import com.trustagro.crm.entity.CrmLeadStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CrmLeadRepository extends JpaRepository<CrmLead, Long> {
    List<CrmLead> findByStatusOrderByCreatedAtDesc(CrmLeadStatus status);
    List<CrmLead> findAllByOrderByCreatedAtDesc();

    @Query("SELECT l FROM CrmLead l WHERE " +
           "(:email IS NOT NULL AND LOWER(l.email) = LOWER(:email)) OR " +
           "(:phone IS NOT NULL AND l.phone = :phone)")
    List<CrmLead> findByEmailOrPhone(@Param("email") String email, @Param("phone") String phone);

    List<CrmLead> findByConvertedClientId(Long clientId);

    long countByStatus(CrmLeadStatus status);

    @Query("SELECT l.status, COUNT(l) FROM CrmLead l GROUP BY l.status")
    List<Object[]> countGroupedByStatus();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM CrmLead l WHERE l.id = :id")
    Optional<CrmLead> findByIdForUpdate(@Param("id") Long id);
}
