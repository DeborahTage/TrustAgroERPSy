package com.trustagro.feed.repository;

import com.trustagro.feed.entity.Silo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiloRepository extends JpaRepository<Silo, Long> {
    List<Silo> findByBarnId(Long barnId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Silo s WHERE s.id = :id")
    Optional<Silo> findByIdForUpdate(@Param("id") Long id);
}
