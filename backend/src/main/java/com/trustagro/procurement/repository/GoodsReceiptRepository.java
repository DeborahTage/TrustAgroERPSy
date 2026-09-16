package com.trustagro.procurement.repository;

import com.trustagro.procurement.entity.GoodsReceipt;
import com.trustagro.procurement.entity.GoodsReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    Optional<GoodsReceipt> findByGrnNumber(String grnNumber);
    List<GoodsReceipt> findByPurchaseOrderIdOrderByCreatedAtDesc(Long purchaseOrderId);
    List<GoodsReceipt> findByStatusOrderByCreatedAtDesc(GoodsReceiptStatus status);
    List<GoodsReceipt> findAllByOrderByCreatedAtDesc();
    long countByStatus(GoodsReceiptStatus status);
}
