package com.trustagro.hr.repository;

import com.trustagro.hr.entity.LeaveRequest;
import com.trustagro.hr.entity.LeaveRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    Optional<LeaveRequest> findByRequestNumber(String requestNumber);
    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    List<LeaveRequest> findByStatusOrderByCreatedAtDesc(LeaveRequestStatus status);
    List<LeaveRequest> findByEmployeeIdAndStatusOrderByCreatedAtDesc(Long employeeId, LeaveRequestStatus status);
    List<LeaveRequest> findByStartDateBetweenOrderByStartDateAsc(LocalDate from, LocalDate to);
    long countByStatus(LeaveRequestStatus status);
    List<LeaveRequest> findByEmployeeIdAndLeaveTypeIdAndStatus(Long employeeId, Long leaveTypeId, LeaveRequestStatus status);
}
