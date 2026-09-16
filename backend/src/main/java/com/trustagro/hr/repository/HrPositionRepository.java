package com.trustagro.hr.repository;

import com.trustagro.hr.entity.HrPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HrPositionRepository extends JpaRepository<HrPosition, Long> {
    Optional<HrPosition> findByCode(String code);
    boolean existsByCode(String code);
    List<HrPosition> findByActiveTrueOrderByNameAsc();
    List<HrPosition> findAllByOrderByNameAsc();
}
