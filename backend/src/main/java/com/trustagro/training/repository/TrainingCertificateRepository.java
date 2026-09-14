package com.trustagro.training.repository;

import com.trustagro.training.entity.CertificateStatus;
import com.trustagro.training.entity.TrainingCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainingCertificateRepository extends JpaRepository<TrainingCertificate, Long> {
    Optional<TrainingCertificate> findByCertificateNumber(String certificateNumber);
    Optional<TrainingCertificate> findByVerificationCode(String verificationCode);
    Optional<TrainingCertificate> findByRegistrationId(Long registrationId);
    boolean existsByRegistrationId(Long registrationId);
    List<TrainingCertificate> findByStatusOrderByCreatedAtDesc(CertificateStatus status);
    long countByStatus(CertificateStatus status);
}
