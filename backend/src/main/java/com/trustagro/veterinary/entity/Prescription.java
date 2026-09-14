package com.trustagro.veterinary.entity;

import com.trustagro.inventory.entity.StockBatch;
import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Canonical veterinary prescription — single source of truth for the
 * Vet → Pharmacy → Inventory → Finance workflow.
 */
@Entity
@Table(name = "prescriptions")
@Data
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true)
    private String prescriptionNumber;

    @Enumerated(EnumType.STRING)
    private PrescriptionType prescriptionType = PrescriptionType.INTERNAL_FARM;

    private Long farmId;
    private Long barnId;
    private Long flockId;
    private Long clientId;
    private Long diseaseCaseId;
    private Long treatmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private com.trustagro.inventory.entity.InventoryItem inventoryItem;

    private String drugName;
    private Double quantity;
    private String unit;
    private String dosageInstruction;
    private String frequency;
    private Integer durationDays;
    private String administrationRoute;
    private Integer withdrawalPeriodDays;
    private LocalDate treatmentStartDate;
    private LocalDate withdrawalEndDate;
    private LocalDate prescribedDate;

    @Enumerated(EnumType.STRING)
    private PrescriptionPriority priority = PrescriptionPriority.NORMAL;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_vet")
    private User createdByVet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispensed_by")
    private User dispensedBy;

    private LocalDateTime dispensedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispensed_batch_id")
    private StockBatch dispensedBatch;

    private String dispensedLotNumber;
    private LocalDate dispensedExpiry;
    private BigDecimal unitCostAtDispense;
    private BigDecimal totalCostAtDispense;

    @Enumerated(EnumType.STRING)
    private PrescriptionStatus status = PrescriptionStatus.DRAFT;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Long getInventoryItemId() {
        return inventoryItem != null ? inventoryItem.getId() : null;
    }

    public void setInventoryItemId(Long inventoryItemId) {
        if (inventoryItemId != null) {
            this.inventoryItem = new com.trustagro.inventory.entity.InventoryItem();
            this.inventoryItem.setId(inventoryItemId);
        } else {
            this.inventoryItem = null;
        }
    }

    public Long getDispensedBatchId() {
        return dispensedBatch != null ? dispensedBatch.getId() : null;
    }
}
