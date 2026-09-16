package com.trustagro.procurement.entity;

import com.trustagro.inventory.entity.PurchaseOrder;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proc_requisitions")
@Data
public class ProcRequisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String requisitionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ProcRequisitionStatus status = ProcRequisitionStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProcRequisitionPriority priority = ProcRequisitionPriority.NORMAL;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String justification;

    private LocalDate neededBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_po_id")
    private PurchaseOrder convertedPurchaseOrder;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
