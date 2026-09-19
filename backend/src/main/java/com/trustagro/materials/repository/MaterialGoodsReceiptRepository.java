package com.trustagro.materials.repository;

import com.trustagro.materials.entity.MaterialGoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialGoodsReceiptRepository extends JpaRepository<MaterialGoodsReceipt, Long> {

    Optional<MaterialGoodsReceipt> findByReceiptNumber(String receiptNumber);

    List<MaterialGoodsReceipt> findByMaterialRequestIdOrderByCreatedAtDesc(Long materialRequestId);
}
