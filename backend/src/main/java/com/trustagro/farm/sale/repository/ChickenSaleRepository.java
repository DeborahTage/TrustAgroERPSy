package com.trustagro.farm.sale.repository;

import com.trustagro.farm.sale.entity.ChickenSale;
import com.trustagro.farm.sale.entity.ChickenSaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChickenSaleRepository extends JpaRepository<ChickenSale, Long> {
    Optional<ChickenSale> findBySaleNumber(String saleNumber);
    List<ChickenSale> findByCustomerIdOrderBySaleDateDesc(Long customerId);
    List<ChickenSale> findByFlockIdOrderBySaleDateDesc(Long flockId);
    List<ChickenSale> findByFarmIdOrderBySaleDateDesc(Long farmId);
    List<ChickenSale> findByStatusOrderBySaleDateDesc(ChickenSaleStatus status);
    long countBySaleNumberStartingWith(String prefix);
}
