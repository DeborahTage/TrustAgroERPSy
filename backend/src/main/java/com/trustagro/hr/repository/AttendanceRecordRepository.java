package com.trustagro.hr.repository;

import com.trustagro.hr.entity.AttendanceRecord;
import com.trustagro.hr.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);
    boolean existsByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate attendanceDate);
    List<AttendanceRecord> findByEmployeeIdOrderByAttendanceDateDesc(Long employeeId);
    List<AttendanceRecord> findByAttendanceDateOrderByEmployeeIdAsc(LocalDate attendanceDate);
    List<AttendanceRecord> findByAttendanceDateBetweenOrderByAttendanceDateDesc(LocalDate from, LocalDate to);
    List<AttendanceRecord> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
            Long employeeId, LocalDate from, LocalDate to);
    long countByAttendanceDateAndStatus(LocalDate attendanceDate, AttendanceStatus status);
    long countByAttendanceDate(LocalDate attendanceDate);
}
