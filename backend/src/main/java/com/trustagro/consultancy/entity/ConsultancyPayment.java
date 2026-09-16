package com.trustagro.consultancy.entity;

import com.trustagro.finance.entity.Invoice;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain payment receipt row for consultancy invoices.
 * Canonical cash/AR posting lives in Finance (TRANSFER), not here.
 */
@Entity
@Table(name = "consultancy_payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consultancy_payment_number", columnNames = "payment_number")
})
@Data
public class ConsultancyPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_number", nullable = false, length = 40)
    private String paymentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "engagement_id", nullable = false)
    private ConsultancyEngagement engagement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false, length = 8)
    private String currency = "ETB";

    private LocalDate paymentDate;
    private String method;
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
