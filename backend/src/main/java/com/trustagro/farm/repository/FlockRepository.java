package com.trustagro.farm.repository;

import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.entity.FlockStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FlockRepository extends JpaRepository<Flock, Long> {
    List<Flock> findByFarmId(Long farmId);
    List<Flock> findByStatus(FlockStatus status);
    long countByStatus(FlockStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Flock f WHERE f.id = :id")
    Optional<Flock> findByIdForUpdate(@Param("id") Long id);
}
