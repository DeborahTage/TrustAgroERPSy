package com.trustagro.inventory.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
@Data
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private StockBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType movementType;

    @Column(nullable = false)
    private Double quantity;

    private BigDecimal unitCost;
    private BigDecimal totalCost;

    private String fromLocation;
    private String toLocation;

    private String referenceType;
    private Long referenceId;
    
    private String reasonCode;
    
    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scanned_by")
    private User scannedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Explicit Getters/Setters to bypass Lombok processing issues
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public InventoryItem getItem() { return item; }
    public void setItem(InventoryItem item) { this.item = item; }
    public StockBatch getBatch() { return batch; }
    public void setBatch(StockBatch batch) { this.batch = batch; }
    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }
    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public void setToLocation(String toLocation) { this.toLocation = toLocation; }
}
