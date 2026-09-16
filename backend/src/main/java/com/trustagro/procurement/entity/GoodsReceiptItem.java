package com.trustagro.procurement.entity;

import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.PurchaseOrderItem;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "proc_goods_receipt_items")
@Data
public class GoodsReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_item_id", nullable = false)
    private PurchaseOrderItem purchaseOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem item;

    @Column(nullable = false)
    private Double receivedQty;

    private Double rejectedQty = 0.0;

    private String batchNumber;
    private LocalDate expiryDate;
    private LocalDate manufactureDate;

    private BigDecimal unitCost;

    /** Set after GRN post via InventoryService.stockIn. */
    private Long stockBatchId;
}
