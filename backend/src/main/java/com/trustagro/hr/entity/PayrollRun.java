package com.trustagro.hr.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_payroll_runs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hr_payroll_run_period_number", columnNames = {"payroll_period_id", "run_number"})
})
@Data
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_period_id", nullable = false)
    private PayrollPeriod payrollPeriod;

    @Column(name = "run_number", nullable = false)
    private int runNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollRunStatus status = PayrollRunStatus.DRAFT;

    @Column(nullable = false)
    private int employeeCount = 0;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal deductionAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 8)
    private String currency = "ETB";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    private LocalDateTime approvedAt;
    private LocalDateTime processedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
