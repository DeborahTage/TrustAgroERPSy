package com.trustagro.materials.entity;

import com.trustagro.inventory.entity.InventoryItem;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "material_goods_receipt_lines")
@Data
public class MaterialGoodsReceiptLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private MaterialGoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_line_id")
    private MaterialRequestLine requestLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal quantityReceived;

    @Column(length = 80)
    private String batchNumber;

    private LocalDate expiryDate;

    @Column(length = 80)
    private String conditionNote;

    private Long stockMovementId;
}
