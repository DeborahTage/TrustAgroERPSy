package com.trustagro.hr.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configurable payroll deduction. The ERP applies whatever rate is configured here —
 * it does not encode any statutory tax or pension schedule. Rates must be set and
 * maintained by the organisation according to the regulations that apply to it.
 */
@Entity
@Table(name = "hr_payroll_deduction_types", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hr_deduction_type_code", columnNames = "code")
})
@Data
public class PayrollDeductionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeductionCategory category = DeductionCategory.OTHER;

    /** Percentage of gross salary. Takes precedence over {@link #fixedAmount} when set. */
    @Column(precision = 8, scale = 4)
    private BigDecimal ratePercent;

    @Column(precision = 19, scale = 2)
    private BigDecimal fixedAmount;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Whether this deduction rate is approved as an official statutory rate.
     * Unapproved rates or placeholder rates block payroll approval, processing, and payment.
     */
    @Column(nullable = false)
    private boolean approved = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
