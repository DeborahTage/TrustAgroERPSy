package com.trustagro.hr.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_payroll_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hr_payroll_item_run_employee", columnNames = {"payroll_run_id", "employee_id"})
})
@Data
public class PayrollItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeProfile employee;

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
    private PayrollItemStatus status = PayrollItemStatus.CALCULATED;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayrollPaymentStatus paymentStatus = PayrollPaymentStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
