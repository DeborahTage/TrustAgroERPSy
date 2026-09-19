package com.trustagro.farm.repository;

import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.FarmStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FarmRepository extends JpaRepository<Farm, Long> {
    List<Farm> findByStatus(FarmStatus status);
    long countByStatus(FarmStatus status);
    List<Farm> findByIdIn(List<Long> ids);
    java.util.Optional<Farm> findByFarmNameIgnoreCase(String farmName);
}
