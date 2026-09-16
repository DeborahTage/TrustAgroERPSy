package com.trustagro.consultancy.entity;

import com.trustagro.crm.entity.CrmClient;
import com.trustagro.finance.entity.Invoice;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultancy_engagements", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consultancy_engagement_number", columnNames = "engagement_number"),
        @UniqueConstraint(name = "uk_consultancy_engagement_request", columnNames = "request_id")
})
@Data
public class ConsultancyEngagement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "engagement_number", nullable = false, length = 40)
    private String engagementNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CrmClient customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ConsultancyServiceCatalog service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposal_id")
    private ConsultancyProposal proposal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private ConsultancyRequest request;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String objectives;

    @Column(columnDefinition = "TEXT")
    private String scope;

    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private LocalDate actualEndDate;
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EngagementStatus status = EngagementStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestPriority priority = RequestPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_consultant_id")
    private User leadConsultant;

    @Column(precision = 14, scale = 2)
    private BigDecimal estimatedAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal agreedAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 8)
    private String currency = "ETB";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingType billingType = BillingType.FIXED_PRICE;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal invoicedAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsultancyPaymentStatus paymentStatus = ConsultancyPaymentStatus.UNPAID;

    /** Primary fixed-price invoice when billed as a whole engagement. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by")
    private User completedBy;

    @Column(columnDefinition = "TEXT")
    private String completionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
