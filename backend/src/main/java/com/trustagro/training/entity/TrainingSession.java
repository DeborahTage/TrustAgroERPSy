package com.trustagro.training.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "training_sessions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_training_session_code", columnNames = "session_code")
})
@Data
public class TrainingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private TrainingProgram program;

    @Column(name = "session_code", nullable = false, length = 40)
    private String sessionCode;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    private String venue;

    @Column(nullable = false)
    private Integer capacity = 0;

    /** Confirmed seats currently reserved — updated under pessimistic lock. */
    @Column(nullable = false)
    private Integer confirmedCount = 0;

    private String trainerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public int remainingCapacity() {
        int cap = capacity != null ? capacity : 0;
        int used = confirmedCount != null ? confirmedCount : 0;
        return Math.max(0, cap - used);
    }
}
