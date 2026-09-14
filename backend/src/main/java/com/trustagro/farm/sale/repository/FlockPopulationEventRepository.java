package com.trustagro.farm.sale.repository;

import com.trustagro.farm.sale.entity.FlockPopulationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlockPopulationEventRepository extends JpaRepository<FlockPopulationEvent, Long> {
    List<FlockPopulationEvent> findByFlockIdOrderByCreatedAtDesc(Long flockId);
    List<FlockPopulationEvent> findByChickenSaleIdOrderByCreatedAtAsc(Long chickenSaleId);
}
