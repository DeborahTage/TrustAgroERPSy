package com.trustagro.farm.repository;

import com.trustagro.farm.entity.FarmNumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FarmNumberSequenceRepository extends JpaRepository<FarmNumberSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM FarmNumberSequence s WHERE s.name = :name")
    Optional<FarmNumberSequence> findByNameForUpdate(@Param("name") String name);
}
