package com.trustagro.hr.repository;

import com.trustagro.hr.entity.PayrollItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PayrollItemRepository extends JpaRepository<PayrollItem, Long> {
    List<PayrollItem> findByPayrollRunIdOrderByIdAsc(Long payrollRunId);
    List<PayrollItem> findByEmployeeIdOrderByIdDesc(Long employeeId);
    Optional<PayrollItem> findByPayrollRunIdAndEmployeeId(Long payrollRunId, Long employeeId);
    void deleteByPayrollRunId(Long payrollRunId);
    long countByPayrollRunId(Long payrollRunId);

    /** Net still owed on payroll that has been posted to Finance but not fully disbursed. */
    @Query("SELECT COALESCE(SUM(i.netSalary - i.paidAmount), 0) FROM PayrollItem i "
            + "WHERE i.status IN (com.trustagro.hr.entity.PayrollItemStatus.PROCESSED, "
            + "com.trustagro.hr.entity.PayrollItemStatus.PAID)")
    BigDecimal sumOutstandingNet();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM PayrollItem i WHERE i.id = :id")
    Optional<PayrollItem> findByIdForUpdate(@Param("id") Long id);
}
