package com.trustagro.procurement.entity;

import com.trustagro.inventory.entity.PurchaseOrder;
import com.trustagro.inventory.entity.Supplier;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proc_supplier_invoices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_supplier_invoice_number",
                columnNames = {"supplier_id", "supplier_invoice_number"})
})
@Data
public class SupplierInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String invoiceNumber;

    /** Supplier's own invoice / bill number — unique per supplier. */
    @Column(name = "supplier_invoice_number", nullable = false, length = 64)
    private String supplierInvoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id")
    private GoodsReceipt goodsReceipt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private SupplierInvoiceStatus status = SupplierInvoiceStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MatchStatus matchStatus = MatchStatus.PENDING;

    private LocalDate invoiceDate;
    private LocalDate dueDate;

    @Column(length = 8)
    private String currency = "ETB";

    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String matchNotes;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public BigDecimal getOutstanding() {
        BigDecimal total = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        return total.subtract(paid);
    }
}
