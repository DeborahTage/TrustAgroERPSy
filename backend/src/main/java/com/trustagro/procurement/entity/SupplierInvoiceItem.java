package com.trustagro.procurement.entity;

import com.trustagro.inventory.entity.InventoryItem;
import com.trustagro.inventory.entity.PurchaseOrderItem;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "proc_supplier_invoice_items")
@Data
public class SupplierInvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_invoice_id", nullable = false)
    private SupplierInvoice supplierInvoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_item_id")
    private PurchaseOrderItem purchaseOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem item;

    private String description;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    private BigDecimal lineTotal;
}
