package com.trustagro.training.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "training_certificates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_training_cert_number", columnNames = "certificate_number"),
        @UniqueConstraint(name = "uk_training_cert_verification", columnNames = "verification_code"),
        @UniqueConstraint(name = "uk_training_cert_registration", columnNames = "registration_id")
})
@Data
public class TrainingCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certificate_number", nullable = false, length = 40)
    private String certificateNumber;

    @Column(name = "verification_code", nullable = false, length = 48)
    private String verificationCode;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private TrainingRegistration registration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private TrainingProgram program;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private TrainingSession session;

    private String participantName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateType certificateType = CertificateType.COMPLETION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CertificateStatus status = CertificateStatus.DRAFT;

    private LocalDate issueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
