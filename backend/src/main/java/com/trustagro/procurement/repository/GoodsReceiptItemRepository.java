package com.trustagro.procurement.repository;

import com.trustagro.procurement.entity.GoodsReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodsReceiptItemRepository extends JpaRepository<GoodsReceiptItem, Long> {
    List<GoodsReceiptItem> findByGoodsReceiptIdOrderByIdAsc(Long goodsReceiptId);
    void deleteByGoodsReceiptId(Long goodsReceiptId);
}
