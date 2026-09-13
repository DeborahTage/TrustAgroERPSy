package com.trustagro.marketing.entity;

import com.trustagro.crm.entity.CrmClient;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_leads")
@Getter
@Setter
public class MarketingLead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;

    private String phone;

    private String email;

    private String organization;

    @Enumerated(EnumType.STRING)
    private LeadSource source;

    private Long campaignId;

    private String notes;

    private boolean converted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crm_client_id")
    private CrmClient crmClient;

    private LocalDateTime createdAt = LocalDateTime.now();
}
