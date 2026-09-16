package com.trustagro.hr.repository;

import com.trustagro.hr.entity.HrDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HrDepartmentRepository extends JpaRepository<HrDepartment, Long> {
    Optional<HrDepartment> findByCode(String code);
    boolean existsByCode(String code);
    List<HrDepartment> findByActiveTrueOrderByNameAsc();
    List<HrDepartment> findAllByOrderByNameAsc();
}
