package com.trustagro.farm.sale.entity;

import com.trustagro.farm.entity.Flock;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "chicken_sale_items")
@Data
public class ChickenSaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private ChickenSale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flock_id", nullable = false)
    private Flock flock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PricingMethod pricingMethod = PricingMethod.PER_BIRD;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 14, scale = 3)
    private BigDecimal weightKg;

    @Column(precision = 14, scale = 2)
    private BigDecimal pricePerKg;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal lineSubtotal = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
