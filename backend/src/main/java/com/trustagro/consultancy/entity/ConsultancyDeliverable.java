package com.trustagro.consultancy.entity;

import com.trustagro.finance.entity.Invoice;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultancy_deliverables", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consultancy_deliverable_number", columnNames = "deliverable_number")
})
@Data
public class ConsultancyDeliverable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deliverable_number", nullable = false, length = 40)
    private String deliverableNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "engagement_id", nullable = false)
    private ConsultancyEngagement engagement;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate dueDate;
    private LocalDate submittedDate;
    private LocalDate approvedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliverableStatus status = DeliverableStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    /** Milestone billable amount (used when engagement billingType = MILESTONE). */
    @Column(precision = 14, scale = 2)
    private BigDecimal billableAmount = BigDecimal.ZERO;

    private boolean billable = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
