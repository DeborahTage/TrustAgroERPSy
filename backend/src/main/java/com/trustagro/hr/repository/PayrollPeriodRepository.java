package com.trustagro.hr.repository;

import com.trustagro.hr.entity.PayrollPeriod;
import com.trustagro.hr.entity.PayrollPeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {
    Optional<PayrollPeriod> findByPeriodCode(String periodCode);
    boolean existsByPeriodCode(String periodCode);
    List<PayrollPeriod> findByStatusOrderByStartDateDesc(PayrollPeriodStatus status);
    List<PayrollPeriod> findAllByOrderByStartDateDesc();
    long countByStatus(PayrollPeriodStatus status);

    /** Any live period whose range intersects [start, end]. Cancelled periods do not block. */
    @Query("SELECT p FROM PayrollPeriod p WHERE p.status <> com.trustagro.hr.entity.PayrollPeriodStatus.CANCELLED "
            + "AND p.startDate <= :end AND p.endDate >= :start")
    List<PayrollPeriod> findOverlapping(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
