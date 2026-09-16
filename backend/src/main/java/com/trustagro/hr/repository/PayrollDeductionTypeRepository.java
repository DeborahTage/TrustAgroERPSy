package com.trustagro.hr.repository;

import com.trustagro.hr.entity.PayrollDeductionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollDeductionTypeRepository extends JpaRepository<PayrollDeductionType, Long> {
    Optional<PayrollDeductionType> findByCode(String code);
    boolean existsByCode(String code);
    List<PayrollDeductionType> findByActiveTrueOrderByCodeAsc();
    List<PayrollDeductionType> findAllByOrderByCodeAsc();
}
