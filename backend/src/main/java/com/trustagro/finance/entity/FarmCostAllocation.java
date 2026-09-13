package com.trustagro.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "farm_cost_allocations")
@Data
public class FarmCostAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long farmId;

    private Long flockId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FarmCostType costType;

    @Column(nullable = false)
    private BigDecimal amount;

    private String referenceType;
    private Long referenceId;
    private LocalDate transactionDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void setCostType(FarmCostType costType) { this.costType = costType; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    public void setFlockId(Long flockId) { this.flockId = flockId; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
    public BigDecimal getAmount() { return amount; }
    public FarmCostType getCostType() { return costType; }
}
