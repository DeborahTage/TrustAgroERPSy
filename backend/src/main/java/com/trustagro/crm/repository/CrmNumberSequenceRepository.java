package com.trustagro.crm.repository;

import com.trustagro.crm.entity.CrmNumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CrmNumberSequenceRepository extends JpaRepository<CrmNumberSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CrmNumberSequence s WHERE s.name = :name")
    Optional<CrmNumberSequence> findByNameForUpdate(@Param("name") String name);
}
