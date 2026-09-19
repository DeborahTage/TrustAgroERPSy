package com.trustagro.materials.repository;

import com.trustagro.materials.entity.MaterialRequest;
import com.trustagro.materials.entity.MaterialRequestStatus;
import com.trustagro.materials.entity.MaterialRequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MaterialRequestRepository extends JpaRepository<MaterialRequest, Long> {

    Optional<MaterialRequest> findByRequestNumber(String requestNumber);

    List<MaterialRequest> findByStatusOrderByCreatedAtDesc(MaterialRequestStatus status);

    List<MaterialRequest> findByStatusInOrderByCreatedAtDesc(Collection<MaterialRequestStatus> statuses);

    List<MaterialRequest> findByTypeAndStatusInOrderByCreatedAtDesc(
            MaterialRequestType type, Collection<MaterialRequestStatus> statuses);

    List<MaterialRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);

    List<MaterialRequest> findByFarmIdOrderByCreatedAtDesc(Long farmId);

    @Query("""
            SELECT DISTINCT mr FROM MaterialRequest mr
            JOIN mr.lines l
            WHERE mr.requester.id = :requesterId
              AND (:farmId IS NULL AND mr.farm IS NULL OR mr.farm.id = :farmId)
              AND l.inventoryItem.id = :itemId
              AND mr.status IN :openStatuses
              AND lower(coalesce(mr.reason, '')) = lower(coalesce(:purpose, ''))
            """)
    List<MaterialRequest> findOpenDuplicates(
            @Param("requesterId") Long requesterId,
            @Param("farmId") Long farmId,
            @Param("itemId") Long itemId,
            @Param("purpose") String purpose,
            @Param("openStatuses") Collection<MaterialRequestStatus> openStatuses);

    @Query("""
            SELECT mr FROM MaterialRequest mr
            WHERE (:status IS NULL OR mr.status = :status)
              AND (:type IS NULL OR mr.type = :type)
              AND (:farmId IS NULL OR mr.farm.id = :farmId)
              AND (:requesterId IS NULL OR mr.requester.id = :requesterId)
              AND (:from IS NULL OR mr.requestDate >= :from)
              AND (:to IS NULL OR mr.requestDate <= :to)
              AND (:department IS NULL OR lower(mr.department) = lower(:department))
            ORDER BY mr.createdAt DESC
            """)
    List<MaterialRequest> filter(
            @Param("status") MaterialRequestStatus status,
            @Param("type") MaterialRequestType type,
            @Param("farmId") Long farmId,
            @Param("requesterId") Long requesterId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("department") String department);

    long countByStatus(MaterialRequestStatus status);

    long countByStatusIn(Collection<MaterialRequestStatus> statuses);

    long countByTypeAndStatusIn(MaterialRequestType type, Collection<MaterialRequestStatus> statuses);
}
