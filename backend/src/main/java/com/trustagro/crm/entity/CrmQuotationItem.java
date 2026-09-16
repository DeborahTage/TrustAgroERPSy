package com.trustagro.crm.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "crm_quotation_items")
@Data
public class CrmQuotationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id", nullable = false)
    private CrmQuotation quotation;

    @Column(nullable = false)
    private String description;

    private String productOrService;

    @Column(nullable = false)
    private Double quantity = 1.0;

    private String unit;

    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;
}
