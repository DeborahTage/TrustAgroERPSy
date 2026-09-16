package com.trustagro.hr.repository;

import com.trustagro.hr.entity.HrNumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HrNumberSequenceRepository extends JpaRepository<HrNumberSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM HrNumberSequence s WHERE s.name = :name")
    Optional<HrNumberSequence> findByNameForUpdate(@Param("name") String name);
}
