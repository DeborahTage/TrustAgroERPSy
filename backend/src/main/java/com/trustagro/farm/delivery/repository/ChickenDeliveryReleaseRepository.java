package com.trustagro.farm.delivery.repository;

import com.trustagro.farm.delivery.entity.ChickenDeliveryRelease;
import com.trustagro.farm.delivery.entity.ChickenReleaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChickenDeliveryReleaseRepository extends JpaRepository<ChickenDeliveryRelease, Long> {
    Optional<ChickenDeliveryRelease> findByReleaseNumber(String releaseNumber);

    List<ChickenDeliveryRelease> findByStatusOrderByCreatedAtDesc(ChickenReleaseStatus status);

    List<ChickenDeliveryRelease> findByStatusInOrderByCreatedAtDesc(List<ChickenReleaseStatus> statuses);

    List<ChickenDeliveryRelease> findByChickenSaleIdOrderByCreatedAtAsc(Long chickenSaleId);

    @Query("SELECT COALESCE(SUM(r.authorizedChickenQuantity), 0) FROM ChickenDeliveryRelease r "
            + "WHERE r.chickenSale.id = :saleId AND r.status NOT IN ('REJECTED', 'FARM_REJECTED', 'CANCELLED')")
    int sumAuthorizedForSale(@Param("saleId") Long saleId);
}
