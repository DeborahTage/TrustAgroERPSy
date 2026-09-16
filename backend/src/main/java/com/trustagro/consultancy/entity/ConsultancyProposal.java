package com.trustagro.consultancy.entity;

import com.trustagro.crm.entity.CrmClient;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultancy_proposals", uniqueConstraints = {
        @UniqueConstraint(name = "uk_consultancy_proposal_number", columnNames = "proposal_number")
})
@Data
public class ConsultancyProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proposal_number", nullable = false, length = 40)
    private String proposalNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CrmClient customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private ConsultancyRequest request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ConsultancyServiceCatalog service;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String scope;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal proposedAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 8)
    private String currency = "ETB";

    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalStatus status = ProposalStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Set when converted to engagement — prevents duplicate conversion. */
    private Long convertedEngagementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
