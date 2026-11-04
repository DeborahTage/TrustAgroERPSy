package com.trustagro.pharmacy.repository;

import com.trustagro.pharmacy.entity.PharmacyPrescription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PharmacyPrescriptionRepository extends JpaRepository<PharmacyPrescription, Long> {
    List<PharmacyPrescription> findByStatus(String status);
    long countByStatus(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PharmacyPrescription p WHERE p.id = :id")
    Optional<PharmacyPrescription> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PharmacyPrescription p WHERE LOWER(p.prescriptionCode) = LOWER(:code)")
    Optional<PharmacyPrescription> findByPrescriptionCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PharmacyPrescription p WHERE p.vetPrescription.id = :vetPrescriptionId")
    Optional<PharmacyPrescription> findByVetPrescriptionIdForUpdate(
            @Param("vetPrescriptionId") Long vetPrescriptionId);
}
