package com.trustagro.materials.entity;

import com.trustagro.farm.entity.Farm;
import com.trustagro.inventory.entity.Supplier;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "material_requests")
@Data
public class MaterialRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String requestNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MaterialRequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MaterialRequestStatus status = MaterialRequestStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(length = 120)
    private String department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id")
    private Farm farm;

    @Column(nullable = false)
    private LocalDate requestDate;

    private LocalDate neededBy;

    @Column(length = 20)
    private String priority = "NORMAL";

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 500)
    private String attachmentUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_supplier_id")
    private Supplier preferredSupplier;

    @Column(precision = 18, scale = 2)
    private BigDecimal estimatedTotal = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2)
    private BigDecimal approvedAmount = BigDecimal.ZERO;

    @Column(precision = 18, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finance_approved_by")
    private User financeApprovedBy;

    private LocalDateTime financeApprovedAt;

    private Long procRequisitionId;
    private Long internalRequisitionId;
    private Long purchaseOrderId;
    private Long lastSupplierPaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private User acknowledgedBy;

    private LocalDateTime acknowledgedAt;

    @Column(columnDefinition = "TEXT")
    private String acknowledgementNotes;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "materialRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<MaterialRequestLine> lines = new ArrayList<>();

    public void addLine(MaterialRequestLine line) {
        lines.add(line);
        line.setMaterialRequest(this);
    }

    public void clearLines() {
        for (MaterialRequestLine line : lines) {
            line.setMaterialRequest(null);
        }
        lines.clear();
    }
}
