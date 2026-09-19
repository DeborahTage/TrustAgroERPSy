package com.trustagro.farm.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "flocks")
@Data
public class Flock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String batchCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    private String birdType;

    private String breed;

    private String sourceHatchery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barn_id")
    private Barn barn;

    private Integer initialBirdCount;

    private Integer currentBirdCount;

    private LocalDate startDate;

    private LocalDate expectedEndDate;

    private LocalDate expectedHarvestDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private com.trustagro.inventory.entity.Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_person_id")
    private com.trustagro.user.entity.User responsiblePerson;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String sourceDocumentUrl;

    @Enumerated(EnumType.STRING)
    private FlockStatus status = FlockStatus.ACTIVE;

    private java.time.LocalDate withdrawalHoldUntil;
    private Boolean isUnderWithdrawal = false;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public String getBatchCode() { return batchCode; }
    public Integer getCurrentBirdCount() { return currentBirdCount; }
    public void setCurrentBirdCount(Integer currentBirdCount) { this.currentBirdCount = currentBirdCount; }
    public Integer getInitialBirdCount() { return initialBirdCount; }
    public void setInitialBirdCount(Integer initialBirdCount) { this.initialBirdCount = initialBirdCount; }

    public String getSourceHatchery() { return sourceHatchery; }
    public String getBreed() { return breed; }
    public LocalDate getStartDate() { return startDate; }
    public Barn getBarn() { return barn; }
    public void setFarm(Farm farm) { this.farm = farm; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public void setBreed(String breed) { this.breed = breed; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setStatus(FlockStatus status) { this.status = status; }
    public Farm getFarm() { return farm; }
    public String getBirdType() { return birdType; }
    public void setExpectedEndDate(LocalDate expectedEndDate) { this.expectedEndDate = expectedEndDate; }
    public LocalDate getExpectedEndDate() { return expectedEndDate; }
    public FlockStatus getStatus() { return status; }
    public LocalDate getWithdrawalHoldUntil() { return withdrawalHoldUntil; }
    public void setWithdrawalHoldUntil(LocalDate date) { this.withdrawalHoldUntil = date; }
    public Boolean getIsUnderWithdrawal() { return isUnderWithdrawal; }
    public void setIsUnderWithdrawal(Boolean isUnderWithdrawal) { this.isUnderWithdrawal = isUnderWithdrawal; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
