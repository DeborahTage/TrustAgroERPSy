package com.trustagro.training.repository;

import com.trustagro.training.entity.RegistrationStatus;
import com.trustagro.training.entity.TrainingPaymentStatus;
import com.trustagro.training.entity.TrainingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TrainingRegistrationRepository extends JpaRepository<TrainingRegistration, Long> {
    Optional<TrainingRegistration> findByRegistrationNumber(String registrationNumber);
    Optional<TrainingRegistration> findBySessionIdAndClientId(Long sessionId, Long clientId);
    boolean existsBySessionIdAndClientId(Long sessionId, Long clientId);
    List<TrainingRegistration> findBySessionIdOrderByCreatedAtDesc(Long sessionId);
    List<TrainingRegistration> findByClientIdOrderByCreatedAtDesc(Long clientId);
    List<TrainingRegistration> findByStatusOrderByCreatedAtDesc(RegistrationStatus status);
    List<TrainingRegistration> findByPaymentStatusOrderByCreatedAtDesc(TrainingPaymentStatus paymentStatus);
    long countByStatus(RegistrationStatus status);
    long countBySessionIdAndStatus(Long sessionId, RegistrationStatus status);
    long countByPaymentStatus(TrainingPaymentStatus paymentStatus);
    long countByCompletedTrue();

    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM TrainingRegistration r WHERE r.status <> com.trustagro.training.entity.RegistrationStatus.CANCELLED")
    java.math.BigDecimal sumRegistrationTotals();

    @Query("SELECT COALESCE(SUM(r.paidAmount), 0) FROM TrainingRegistration r WHERE r.status <> com.trustagro.training.entity.RegistrationStatus.CANCELLED")
    java.math.BigDecimal sumPaidAmounts();

    @Query("SELECT COALESCE(SUM(r.outstandingAmount), 0) FROM TrainingRegistration r WHERE r.status <> com.trustagro.training.entity.RegistrationStatus.CANCELLED")
    java.math.BigDecimal sumOutstanding();
}
