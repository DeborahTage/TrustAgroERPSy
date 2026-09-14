package com.trustagro.farm.dto;

import com.trustagro.farm.entity.FlockStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FlockResponse {
    private Long id;
    private String batchCode;
    private Long farmId;
    private String farmName;
    private Long barnId;
    private String barnName;
    private String birdType;
    private Integer initialBirdCount;
    private Integer currentBirdCount;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private FlockStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void setId(Long id) { this.id = id; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
    public void setFarmName(String farmName) { this.farmName = farmName; }
    public void setBarnId(Long barnId) { this.barnId = barnId; }
    public void setBarnName(String barnName) { this.barnName = barnName; }
    public void setBirdType(String birdType) { this.birdType = birdType; }
    public void setInitialBirdCount(Integer initialBirdCount) { this.initialBirdCount = initialBirdCount; }
    public void setCurrentBirdCount(Integer currentBirdCount) { this.currentBirdCount = currentBirdCount; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setExpectedEndDate(LocalDate expectedEndDate) { this.expectedEndDate = expectedEndDate; }
    public void setStatus(FlockStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
