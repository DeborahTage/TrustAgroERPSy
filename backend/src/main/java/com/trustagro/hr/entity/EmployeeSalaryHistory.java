package com.trustagro.hr.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Append-only compensation trail. Payroll resolves the salary effective on the
 * period end date from here, so past runs never move when a salary changes.
 */
@Entity
@Table(name = "hr_salary_history", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hr_salary_employee_effective", columnNames = {"employee_id", "effective_date"})
})
@Data
public class EmployeeSalaryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeProfile employee;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(nullable = false, length = 8)
    private String currency = "ETB";

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
