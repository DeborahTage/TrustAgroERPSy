package com.trustagro.inventory.repository;

import com.trustagro.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByItemId(Long itemId);
    List<StockMovement> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
    List<StockMovement> findByReferenceTypeAndReferenceIdIn(String referenceType, Collection<Long> referenceIds);

    @Query("SELECT m FROM StockMovement m WHERE m.item.id = :itemId AND m.movementType = 'SALE' AND m.createdAt >= :since ORDER BY m.createdAt ASC")
    List<StockMovement> findRecentSalesByItemId(@Param("itemId") Long itemId, @Param("since") LocalDateTime since);

    long countByMovementTypeAndCreatedAtAfter(com.trustagro.inventory.entity.MovementType type, LocalDateTime since);

    @Query("SELECT m FROM StockMovement m WHERE m.movementType = 'RECEIPT' ORDER BY m.createdAt DESC")
    List<StockMovement> findLatestStockIn(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT DATE(m.createdAt) as date, SUM(m.quantity) as count FROM StockMovement m WHERE m.movementType = :type AND m.createdAt >= :since GROUP BY DATE(m.createdAt) ORDER BY DATE(m.createdAt) ASC")
    List<Object[]> findDailyMovements(@Param("type") com.trustagro.inventory.entity.MovementType type, @Param("since") LocalDateTime since);
}
