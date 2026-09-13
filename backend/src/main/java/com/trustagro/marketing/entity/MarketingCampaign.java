package com.trustagro.marketing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketing_campaigns")
@Getter
@Setter
public class MarketingCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    private LeadSource channel;

    private BigDecimal budget = BigDecimal.ZERO;

    private BigDecimal spent = BigDecimal.ZERO;

    private Integer leadsGenerated = 0;

    private Integer conversions = 0;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDateTime createdAt = LocalDateTime.now();
}
