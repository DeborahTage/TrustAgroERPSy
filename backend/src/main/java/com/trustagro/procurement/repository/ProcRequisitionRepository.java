package com.trustagro.procurement.repository;

import com.trustagro.procurement.entity.ProcRequisition;
import com.trustagro.procurement.entity.ProcRequisitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProcRequisitionRepository extends JpaRepository<ProcRequisition, Long> {
    Optional<ProcRequisition> findByRequisitionNumber(String requisitionNumber);
    List<ProcRequisition> findByStatusOrderByCreatedAtDesc(ProcRequisitionStatus status);
    List<ProcRequisition> findAllByOrderByCreatedAtDesc();
    long countByStatus(ProcRequisitionStatus status);
}
