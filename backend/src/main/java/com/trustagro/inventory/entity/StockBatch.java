package com.trustagro.inventory.entity;

import com.trustagro.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_batches", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"inventory_item_id", "batch_number"})
})
@Data
public class StockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem item;

    @Column(nullable = false)
    private String batchNumber;

    @Column(nullable = false)
    private Double quantityReceived;

    @Column(nullable = false)
    private Double quantityRemaining;

    private Double quantityReserved = 0.0;

    private BigDecimal unitCost;

    private LocalDate expiryDate;
    private LocalDate manufacturingDate;

    @Column(nullable = false)
    private LocalDate receivedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private User receivedBy;

    private String poReference;
    private String storageZone;
    private String shelfLocation;

    @Enumerated(EnumType.STRING)
    private BatchStatus status = BatchStatus.AVAILABLE;

    private boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Explicit accessors
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public Double getQuantityRemaining() { return quantityRemaining; }
    public void setQuantityRemaining(Double quantityRemaining) { this.quantityRemaining = quantityRemaining; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public InventoryItem getItem() { return item; }
    public void setItem(InventoryItem item) { this.item = item; }
    public String getStorageZone() { return storageZone; }
    public void setStorageZone(String storageZone) { this.storageZone = storageZone; }
    public String getShelfLocation() { return shelfLocation; }

    public void setReceivedBy(User receivedBy) { this.receivedBy = receivedBy; }
    public void setStatus(BatchStatus status) { this.status = status; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    public void setQuantityReceived(Double quantityReceived) { this.quantityReceived = quantityReceived; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
    public void setPoReference(String poReference) { this.poReference = poReference; }
    public String getPoReference() { return poReference; }
    public void setManufacturingDate(LocalDate manufacturingDate) { this.manufacturingDate = manufacturingDate; }
    public LocalDate getManufacturingDate() { return manufacturingDate; }
}
