package com.trustagro.procurement.repository;

import com.trustagro.procurement.entity.ProcNumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProcNumberSequenceRepository extends JpaRepository<ProcNumberSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ProcNumberSequence s WHERE s.name = :name")
    Optional<ProcNumberSequence> findByNameForUpdate(@Param("name") String name);
}
