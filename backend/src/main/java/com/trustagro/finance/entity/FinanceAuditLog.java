package com.trustagro.finance.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable audit record created every time a FinanceTransaction is posted.
 * Provides a tamper-evident history for every double-entry posting.
 */
@Entity
@Table(name = "finance_audit_logs")
@Getter
@Setter
public class FinanceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link back to the transaction that was posted
    @Column(nullable = false)
    private Long transactionId;

    // Human-readable summary of the operation
    @Column(nullable = false, length = 512)
    private String action;          // e.g. "POSTED", "REVERSED"

    @Column(nullable = false)
    private String debitAccount;    // account code

    @Column(nullable = false)
    private String creditAccount;   // account code

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String sourceModule;    // PHARMACY, INVENTORY, MANUAL…

    @Column
    private String referenceType;

    @Column
    private Long referenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(nullable = false)
    private LocalDateTime postedAt;

    @PrePersist
    protected void onPersist() {
        if (postedAt == null) postedAt = LocalDateTime.now();
    }
}
