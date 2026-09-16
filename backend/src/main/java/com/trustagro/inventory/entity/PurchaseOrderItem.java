package com.trustagro.inventory.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
@Data
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem item;

    @Column(nullable = false)
    private Double quantityOrdered;

    private Double quantityReceived = 0.0;

    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public void setItem(InventoryItem item) { this.item = item; }
    public void setQuantityOrdered(Double quantityOrdered) { this.quantityOrdered = quantityOrdered; }
    public void setQuantityReceived(Double quantityReceived) { this.quantityReceived = quantityReceived; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Double getQuantityOrdered() { return quantityOrdered; }
    public Double getQuantityReceived() { return quantityReceived != null ? quantityReceived : 0.0; }
    public Long getId() { return id; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public InventoryItem getItem() { return item; }
    public BigDecimal getLineTotal() { return lineTotal; }
}
