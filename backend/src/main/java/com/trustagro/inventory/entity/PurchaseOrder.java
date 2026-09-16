package com.trustagro.inventory.entity;

import com.trustagro.procurement.entity.ProcRequisition;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_orders")
@Data
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Convert(converter = PurchaseOrderStatusConverter.class)
    @Column(nullable = false, length = 32)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    private LocalDate orderDate;

    /** CASH, NET_7, NET_15, NET_30, NET_60 */
    private String paymentTerms;

    @Column(length = 8)
    private String currency = "ETB";

    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    private LocalDate expectedDelivery;
    private LocalDate actualDelivery;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisition_id")
    private ProcRequisition requisition;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public void setStatus(PurchaseOrderStatus status) { this.status = status; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public String getPoNumber() { return poNumber; }
    public Long getId() { return id; }
    public PurchaseOrderStatus getStatus() { return status; }
    public Supplier getSupplier() { return supplier; }
    public void setActualDelivery(LocalDate actualDelivery) { this.actualDelivery = actualDelivery; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDate getExpectedDelivery() { return expectedDelivery; }
    public void setExpectedDelivery(LocalDate expectedDelivery) { this.expectedDelivery = expectedDelivery; }
    public User getCreatedBy() { return createdBy; }
    public ProcRequisition getRequisition() { return requisition; }
    public void setRequisition(ProcRequisition requisition) { this.requisition = requisition; }
}
