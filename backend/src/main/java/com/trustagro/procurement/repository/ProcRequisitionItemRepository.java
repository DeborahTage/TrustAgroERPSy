package com.trustagro.procurement.repository;

import com.trustagro.procurement.entity.ProcRequisitionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcRequisitionItemRepository extends JpaRepository<ProcRequisitionItem, Long> {
    List<ProcRequisitionItem> findByRequisitionIdOrderByIdAsc(Long requisitionId);
    void deleteByRequisitionId(Long requisitionId);
}
