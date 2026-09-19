package com.trustagro.farm.delivery.repository;

import com.trustagro.farm.delivery.entity.InternalDeliveryStatus;
import com.trustagro.farm.delivery.entity.InternalStockDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InternalStockDeliveryRepository extends JpaRepository<InternalStockDelivery, Long> {
    Optional<InternalStockDelivery> findByReceiptNumber(String receiptNumber);
    List<InternalStockDelivery> findByStatusOrderByCreatedAtDesc(InternalDeliveryStatus status);
}
