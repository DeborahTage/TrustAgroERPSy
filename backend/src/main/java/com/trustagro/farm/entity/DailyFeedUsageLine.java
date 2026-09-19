package com.trustagro.farm.entity;

import com.trustagro.inventory.entity.InventoryItem;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "daily_feed_usage_lines")
@Data
public class DailyFeedUsageLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_record_id", nullable = false)
    private DailyFarmRecord dailyRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    @Column(length = 40)
    private String feedType;

    @Column(precision = 14, scale = 3)
    private BigDecimal issuedQuantity = BigDecimal.ZERO;

    @Column(precision = 14, scale = 3)
    private BigDecimal returnedQuantity = BigDecimal.ZERO;

    @Column(precision = 14, scale = 3)
    private BigDecimal consumedQuantity = BigDecimal.ZERO;

    @Column(length = 20)
    private String unit;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Long stockMovementId;
}
