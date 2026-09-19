package com.trustagro.farm.delivery.entity;

import com.trustagro.crm.entity.CrmClient;
import com.trustagro.farm.entity.Farm;
import com.trustagro.farm.entity.Flock;
import com.trustagro.farm.sale.entity.ChickenSale;
import com.trustagro.farm.sale.entity.ChickenSalePayment;
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
@Table(name = "chicken_delivery_releases")
@Data
public class ChickenDeliveryRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String releaseNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chicken_sale_id", nullable = false)
    private ChickenSale chickenSale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chicken_sale_payment_id")
    private ChickenSalePayment chickenSalePayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flock_id")
    private Flock flock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CrmClient customer;

    @Column(precision = 14, scale = 2)
    private BigDecimal paymentAmount;

    @Column(nullable = false)
    private Integer authorizedChickenQuantity;

    @Column(nullable = false)
    private Integer bonusQuantity = 0;

    @Column(length = 80)
    private String chickenType;

    @Column(length = 500)
    private String paymentReceiptUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChickenReleaseStatus status = ChickenReleaseStatus.SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_approved_by")
    private User farmApprovedBy;

    private LocalDateTime farmApprovedAt;

    @Column(columnDefinition = "TEXT")
    private String farmRejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatched_by")
    private User dispatchedBy;

    private LocalDateTime dispatchedAt;

    private LocalDate dispatchDate;

    @Column(columnDefinition = "TEXT")
    private String marketingNotes;

    @Column(length = 120)
    private String deliveryOutcome;

    @Column(nullable = false)
    private boolean stockDecremented = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
