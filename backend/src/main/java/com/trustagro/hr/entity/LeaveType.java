package com.trustagro.hr.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_leave_types", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hr_leave_type_code", columnNames = "code")
})
@Data
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** When true, approvals draw down a per-year {@link LeaveBalance}. */
    @Column(nullable = false)
    private boolean balanceControlled = true;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal defaultAnnualEntitlement = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean paid = true;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
