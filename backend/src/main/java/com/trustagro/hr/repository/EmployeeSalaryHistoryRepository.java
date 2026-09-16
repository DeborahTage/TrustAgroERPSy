package com.trustagro.hr.repository;

import com.trustagro.hr.entity.EmployeeSalaryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeSalaryHistoryRepository extends JpaRepository<EmployeeSalaryHistory, Long> {
    List<EmployeeSalaryHistory> findByEmployeeIdOrderByEffectiveDateDesc(Long employeeId);
    boolean existsByEmployeeIdAndEffectiveDate(Long employeeId, LocalDate effectiveDate);

    /** Salary in force on a given date — first row wins. */
    @Query("SELECT h FROM EmployeeSalaryHistory h WHERE h.employee.id = :employeeId "
            + "AND h.effectiveDate <= :onDate ORDER BY h.effectiveDate DESC, h.id DESC")
    List<EmployeeSalaryHistory> findEffectiveOn(@Param("employeeId") Long employeeId,
                                                @Param("onDate") LocalDate onDate);
}
