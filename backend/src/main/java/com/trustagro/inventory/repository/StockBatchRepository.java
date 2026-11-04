package com.trustagro.inventory.repository;

import com.trustagro.inventory.entity.StockBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {
    List<StockBatch> findByItemIdAndQuantityRemainingGreaterThanOrderByExpiryDateAscCreatedAtAsc(Long itemId, Double qty);

    @Query("SELECT COALESCE(SUM(b.quantityRemaining), 0) FROM StockBatch b WHERE b.item.id = :itemId AND b.quantityRemaining > 0")
    Double getTotalStock(@Param("itemId") Long itemId);

    @Query("SELECT b FROM StockBatch b WHERE b.item.id = :itemId AND b.quantityRemaining > 0 AND (b.expiryDate IS NULL OR b.expiryDate >= :today) ORDER BY b.expiryDate ASC NULLS LAST, b.createdAt ASC")
    List<StockBatch> findAvailableBatchesFEFO(@Param("itemId") Long itemId, @Param("today") LocalDate today);

    @Query("SELECT b FROM StockBatch b WHERE b.expiryDate IS NOT NULL AND b.expiryDate BETWEEN :today AND :warningDate AND b.quantityRemaining > 0")
    List<StockBatch> findExpiringBatches(@Param("today") LocalDate today, @Param("warningDate") LocalDate warningDate);

    List<StockBatch> findByItemIdOrderByReceivedDateAsc(Long itemId);

    List<StockBatch> findByExpiryDate(LocalDate expiryDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM StockBatch b WHERE b.id IN :ids ORDER BY b.id")
    List<StockBatch> findAllByIdForUpdate(@Param("ids") List<Long> ids);
}
