package com.trustagro.hr.repository;

import com.trustagro.hr.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    Optional<Payslip> findByPayrollItemId(Long payrollItemId);
    Optional<Payslip> findByPayslipNumber(String payslipNumber);
    List<Payslip> findByEmployeeIdOrderByIdDesc(Long employeeId);
    List<Payslip> findByRunIdOrderByIdAsc(Long runId);
    List<Payslip> findByPeriodIdOrderByIdAsc(Long periodId);
    void deleteByRunId(Long runId);
}
