package com.trustagro.training.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "training_programs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_training_program_code", columnNames = "program_code")
})
@Data
public class TrainingProgram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "program_code", nullable = false, length = 40)
    private String programCode;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainingCategory category = TrainingCategory.OTHER;

    @Column(columnDefinition = "TEXT")
    private String objectives;

    /** Duration in hours (or days if durationUnit = DAYS). */
    private Integer durationHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryMode deliveryMode = DeliveryMode.ONSITE;

    private String location;
    private String trainerName;

    private Integer defaultCapacity;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false, length = 8)
    private String currency = "ETB";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgramStatus status = ProgramStatus.DRAFT;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registrationDeadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
