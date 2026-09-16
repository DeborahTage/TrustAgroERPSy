package com.trustagro.crm.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_opportunities", uniqueConstraints = {
        @UniqueConstraint(name = "uk_crm_opportunity_number", columnNames = "opportunity_number")
})
@Data
public class CrmOpportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "opportunity_number", nullable = false, length = 40)
    private String opportunityNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private CrmClient client;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 14, scale = 2)
    private BigDecimal estimatedValue = BigDecimal.ZERO;

    @Column(length = 8)
    private String currency = "ETB";

    /** 0–100 */
    private Integer probability = 10;

    private LocalDate expectedCloseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_owner_id")
    private User salesOwner;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CrmLeadSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OpportunityStage stage = OpportunityStage.PROSPECTING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CrmPriority priority = CrmPriority.MEDIUM;

    private String lossReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private CrmLead lead;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
