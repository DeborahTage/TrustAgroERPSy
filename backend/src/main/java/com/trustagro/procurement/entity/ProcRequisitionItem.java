package com.trustagro.procurement.entity;

import com.trustagro.inventory.entity.InventoryItem;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "proc_requisition_items")
@Data
public class ProcRequisitionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id", nullable = false)
    private ProcRequisition requisition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem item;

    @Column(nullable = false)
    private Double quantityRequested;

    private BigDecimal estimatedUnitPrice;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
