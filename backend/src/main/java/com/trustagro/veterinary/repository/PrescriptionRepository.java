package com.trustagro.veterinary.repository;

import com.trustagro.veterinary.entity.Prescription;
import com.trustagro.veterinary.entity.PrescriptionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByStatus(PrescriptionStatus status);
    List<Prescription> findByFarmId(Long farmId);
    List<Prescription> findByFlockId(Long flockId);
    boolean existsByPrescriptionNumber(String prescriptionNumber);
    long countByStatus(PrescriptionStatus status);
    boolean existsByFlockIdAndWithdrawalEndDateAfter(Long flockId, LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Prescription p WHERE p.id = :id")
    java.util.Optional<Prescription> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Prescription p WHERE LOWER(p.prescriptionNumber) = LOWER(:number)")
    java.util.Optional<Prescription> findByPrescriptionNumberForUpdate(@Param("number") String number);
}
