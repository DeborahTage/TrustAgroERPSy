package com.trustagro.inventory.repository;

import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.ItemStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findByStatus(ItemStatus status);
    Optional<InventoryItem> findByBarcodeOrRfidTag(String barcode, String rfidTag);
    Optional<InventoryItem> findBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.id IN :ids ORDER BY i.id")
    List<InventoryItem> findAllByIdForUpdate(@Param("ids") List<Long> ids);

    @Query("SELECT COUNT(i) FROM InventoryItem i WHERE i.currentStock < 0")
    long countNegativeStock();
}
