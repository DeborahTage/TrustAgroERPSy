package com.trustagro.crm.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crm_leads", uniqueConstraints = {
        @UniqueConstraint(name = "uk_crm_lead_number", columnNames = "lead_number")
})
@Data
public class CrmLead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_number", nullable = false, length = 40)
    private String leadNumber;

    @Column(nullable = false)
    private String name;

    private String company;
    private String contactPerson;
    private String phone;
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CrmLeadSource source = CrmLeadSource.OTHER;

    private String industry;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_owner_id")
    private User assignedOwner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CrmLeadStatus status = CrmLeadStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CrmPriority priority = CrmPriority.MEDIUM;

    private LocalDateTime convertedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_client_id")
    private CrmClient convertedClient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_opportunity_id")
    private CrmOpportunity convertedOpportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_by")
    private User convertedBy;

    /** Optional link back to marketing.MarketingLead.id */
    private Long marketingLeadId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
