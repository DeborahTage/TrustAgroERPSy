package com.trustagro.consultancy.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultancy_assignments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consultancy_assignment_engagement_user", columnNames = {"engagement_id", "consultant_user_id"})
})
@Data
public class ConsultancyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "engagement_id", nullable = false)
    private ConsultancyEngagement engagement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consultant_user_id", nullable = false)
    private User consultant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentRole role = AssignmentRole.SUPPORT_CONSULTANT;

    private LocalDate assignedDate;
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
