package com.trustagro.hr.repository;

import com.trustagro.hr.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    Optional<LeaveType> findByCode(String code);
    boolean existsByCode(String code);
    List<LeaveType> findByActiveTrueOrderByNameAsc();
    List<LeaveType> findAllByOrderByNameAsc();
}
