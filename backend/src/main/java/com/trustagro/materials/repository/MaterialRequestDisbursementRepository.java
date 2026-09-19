package com.trustagro.materials.repository;

import com.trustagro.materials.entity.MaterialRequestDisbursement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MaterialRequestDisbursementRepository extends JpaRepository<MaterialRequestDisbursement, Long> {

    Optional<MaterialRequestDisbursement> findByDisbursementNumber(String disbursementNumber);

    List<MaterialRequestDisbursement> findByMaterialRequestIdOrderByCreatedAtDesc(Long materialRequestId);

    boolean existsByMaterialRequestIdAndAmount(Long materialRequestId, BigDecimal amount);
}
