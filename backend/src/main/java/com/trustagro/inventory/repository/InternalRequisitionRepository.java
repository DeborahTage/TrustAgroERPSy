package com.trustagro.inventory.repository;

import com.trustagro.inventory.entity.InternalRequisition;
import com.trustagro.inventory.entity.RequisitionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InternalRequisitionRepository extends JpaRepository<InternalRequisition, Long> {
    long countByStatus(RequisitionStatus status);
}
