package com.trustagro.hr.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_payroll_periods", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hr_payroll_period_code", columnNames = "period_code")
})
@Data
public class PayrollPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_code", nullable = false, length = 40)
    private String periodCode;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollPeriodStatus status = PayrollPeriodStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private LocalDateTime calculatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculated_by")
    private User calculatedBy;

    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    private LocalDateTime closedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
