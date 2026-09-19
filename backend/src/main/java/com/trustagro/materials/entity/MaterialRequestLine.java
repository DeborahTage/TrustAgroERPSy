package com.trustagro.materials.entity;

import com.trustagro.inventory.entity.InventoryItem;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "material_request_lines")
@Data
public class MaterialRequestLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_request_id", nullable = false)
    private MaterialRequest materialRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityRequested;

    @Column(precision = 14, scale = 3)
    private BigDecimal quantityApproved = BigDecimal.ZERO;

    @Column(precision = 14, scale = 3)
    private BigDecimal quantityOrdered = BigDecimal.ZERO;

    @Column(precision = 14, scale = 3)
    private BigDecimal quantityReceived = BigDecimal.ZERO;

    @Column(precision = 14, scale = 3)
    private BigDecimal quantityIssued = BigDecimal.ZERO;

    @Column(length = 40)
    private String unit;

    @Column(precision = 18, scale = 4)
    private BigDecimal estimatedUnitPrice;

    @Column(precision = 18, scale = 2)
    private BigDecimal estimatedLineTotal;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Long lastStockMovementId;
}
