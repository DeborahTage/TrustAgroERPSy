package com.trustagro.crm.entity;

import com.trustagro.finance.entity.Invoice;
import com.trustagro.finance.entity.PaymentMethod;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain payment receipt for CRM customer orders.
 * Canonical cash/AR posting lives in Finance (TRANSFER), not here.
 */
@Entity
@Table(name = "crm_order_payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_crm_order_payment_number", columnNames = "payment_number"),
        @UniqueConstraint(name = "uk_crm_order_payment_idempotency", columnNames = "idempotency_key")
})
@Data
public class CrmOrderPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_number", nullable = false, length = 40)
    private String paymentNumber;

    /** Optional client-supplied key; nullable unique (multiple nulls allowed). */
    @Column(name = "idempotency_key", length = 80)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod method;

    private LocalDate paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    private Long financeTransactionId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
