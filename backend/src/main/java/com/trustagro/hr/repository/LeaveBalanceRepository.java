package com.trustagro.hr.repository;

import com.trustagro.hr.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(Long employeeId, Long leaveTypeId, int year);
    List<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, int year);
    List<LeaveBalance> findByEmployeeId(Long employeeId);
    List<LeaveBalance> findByYear(int year);
}
