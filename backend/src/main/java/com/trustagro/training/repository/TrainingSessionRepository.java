package com.trustagro.training.repository;

import com.trustagro.training.entity.SessionStatus;
import com.trustagro.training.entity.TrainingSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {
    Optional<TrainingSession> findBySessionCode(String sessionCode);
    boolean existsBySessionCode(String sessionCode);
    List<TrainingSession> findByProgramIdOrderByStartDateAsc(Long programId);
    List<TrainingSession> findByStatusOrderByStartDateAsc(SessionStatus status);
    List<TrainingSession> findByStartDateGreaterThanEqualOrderByStartDateAsc(LocalDate from);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TrainingSession s WHERE s.id = :id")
    Optional<TrainingSession> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(SessionStatus status);
}
