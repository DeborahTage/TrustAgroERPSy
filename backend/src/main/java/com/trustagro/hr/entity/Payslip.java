package com.trustagro.hr.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Immutable snapshot of a payroll item at calculation time. Amounts are copied,
 * not referenced, so a later recalculation cannot rewrite an issued payslip.
 */
@Entity
@Table(name = "hr_payslips", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hr_payslip_item", columnNames = "payroll_item_id"),
        @UniqueConstraint(name = "uk_hr_payslip_number", columnNames = "payslip_number")
})
@Data
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payslip_number", nullable = false, length = 40)
    private String payslipNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_item_id", nullable = false)
    private PayrollItem payrollItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeProfile employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_period_id", nullable = false)
    private PayrollPeriod period;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun run;

    private LocalDate periodStartDate;
    private LocalDate periodEndDate;

    // Snapshot amounts
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal allowances = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal overtime = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal bonuses = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal grossSalary = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal tax = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal pension = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal netSalary = BigDecimal.ZERO;

    @Column(nullable = false, length = 8)
    private String currency = "ETB";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollPaymentStatus paymentStatus = PayrollPaymentStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
