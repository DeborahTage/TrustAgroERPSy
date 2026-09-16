package com.trustagro.inventory.repository;

import com.trustagro.inventory.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    long countByStatus(com.trustagro.inventory.entity.PurchaseOrderStatus status);
}
