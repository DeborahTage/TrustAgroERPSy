package com.trustagro.hr.repository;

import com.trustagro.hr.entity.PayrollRun;
import com.trustagro.hr.entity.PayrollRunStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    List<PayrollRun> findByPayrollPeriodIdOrderByRunNumberDesc(Long payrollPeriodId);
    Optional<PayrollRun> findByPayrollPeriodIdAndRunNumber(Long payrollPeriodId, int runNumber);
    List<PayrollRun> findByStatus(PayrollRunStatus status);

    @Query("SELECT COALESCE(MAX(r.runNumber), 0) FROM PayrollRun r WHERE r.payrollPeriod.id = :periodId")
    int maxRunNumber(@Param("periodId") Long periodId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM PayrollRun r WHERE r.id = :id")
    Optional<PayrollRun> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(PayrollRunStatus status);
}
