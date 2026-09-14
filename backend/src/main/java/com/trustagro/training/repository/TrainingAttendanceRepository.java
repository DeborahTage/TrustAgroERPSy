package com.trustagro.training.repository;

import com.trustagro.training.entity.TrainingAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainingAttendanceRepository extends JpaRepository<TrainingAttendance, Long> {
    List<TrainingAttendance> findByRegistrationIdOrderBySessionDateAsc(Long registrationId);
    List<TrainingAttendance> findBySessionIdOrderBySessionDateAsc(Long sessionId);
    Optional<TrainingAttendance> findByRegistrationIdAndSessionDate(Long registrationId, LocalDate sessionDate);
    long countByRegistrationIdAndStatus(Long registrationId, com.trustagro.training.entity.AttendanceStatus status);
}
