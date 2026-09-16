package com.trustagro.procurement.entity;

import com.trustagro.inventory.entity.PurchaseOrder;
import com.trustagro.inventory.entity.Supplier;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proc_goods_receipts")
@Data
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String grnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GoodsReceiptStatus status = GoodsReceiptStatus.DRAFT;

    private LocalDate receiptDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private User receivedBy;

    private LocalDateTime postedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
