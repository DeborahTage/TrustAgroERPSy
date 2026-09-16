package com.trustagro.procurement.entity;

import com.trustagro.finance.entity.PaymentMethod;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain payment record for AP settlement and finance idempotency.
 * Not a general ledger entry — ledger posting uses MODULE_PROCUREMENT / REF_PROCUREMENT_PAYMENT / id.
 */
@Entity
@Table(name = "proc_supplier_payments")
@Data
public class SupplierPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String paymentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_invoice_id", nullable = false)
    private SupplierInvoice supplierInvoice;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentMethod paymentMethod;

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
