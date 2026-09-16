package com.trustagro.crm.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "crm_quotations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_crm_quotation_number", columnNames = "quotation_number")
})
@Data
public class CrmQuotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quotation_number", nullable = false, length = 40)
    private String quotationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private CrmClient client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    private CrmOpportunity opportunity;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QuotationType quotationType = QuotationType.PRODUCT;

    private LocalDate issueDate;
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuotationStatus status = QuotationStatus.DRAFT;

    @Column(length = 8)
    private String currency = "ETB";

    @Column(precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private Long convertedOrderId;

    /** When CONSULTANCY quotation is accepted, optional link to ConsultancyRequest. */
    private Long consultancyRequestId;

    private Long consultancyEngagementId;

    private Long consultancyContractId;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CrmQuotationItem> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addItem(CrmQuotationItem item) {
        items.add(item);
        item.setQuotation(this);
    }
}
