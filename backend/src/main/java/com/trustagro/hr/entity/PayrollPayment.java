package com.trustagro.hr.entity;

import com.trustagro.finance.entity.PaymentMethod;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A disbursement record, not a ledger. The accounting effect lives entirely in
 * Finance — this row only exists so the posting has a stable referenceId.
 */
@Entity
@Table(name = "hr_payroll_payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_hr_payroll_payment_number", columnNames = "payment_number")
})
@Data
public class PayrollPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_number", nullable = false, length = 40)
    private String paymentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    /** Null when the payment settles the whole run in one batch. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_item_id")
    private PayrollItem payrollItem;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.BANK_TRANSFER;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
