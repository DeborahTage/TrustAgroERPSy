package com.trustagro.inventory.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * @deprecated Dead single-item stub. Use {@code com.trustagro.procurement.entity.ProcRequisition}
 *             ({@code proc_requisitions} / {@code proc_requisition_items}) instead.
 */
@Deprecated
@Entity
@Table(name = "purchase_requisitions")
@Data
public class PurchaseRequisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    private Double requestedQty;

    @Enumerated(EnumType.STRING)
    private RequisitionStatus status = RequisitionStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
