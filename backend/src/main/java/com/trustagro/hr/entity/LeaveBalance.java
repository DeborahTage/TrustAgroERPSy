package com.trustagro.hr.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_leave_balances", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hr_leave_balance_employee_type_year",
                columnNames = {"employee_id", "leave_type_id", "balance_year"})
})
@Data
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeProfile employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    /** Mapped to balance_year — "year" is a reserved word in H2 and PostgreSQL DDL. */
    @Column(name = "balance_year", nullable = false)
    private int year;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal entitlement = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal usedDays = BigDecimal.ZERO;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal remainingDays = BigDecimal.ZERO;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void recalculateRemaining() {
        BigDecimal ent = entitlement != null ? entitlement : BigDecimal.ZERO;
        BigDecimal used = usedDays != null ? usedDays : BigDecimal.ZERO;
        this.remainingDays = ent.subtract(used).setScale(2, RoundingMode.HALF_UP);
    }
}
