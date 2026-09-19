package com.trustagro.farm.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DailyFarmRecordResponse {
    private Long id;
    private LocalDate date;
    private Long farmId;
    private String farmName;
    private Long flockId;
    private String batchCode;
    private Integer openingBirdCount;
    private Integer mortality;
    private Integer culledChickens;
    private BigDecimal feedConsumed;
    private BigDecimal waterConsumed;
    private BigDecimal averageWeight;
    private String symptomsOrRemarks;
    private com.trustagro.veterinary.entity.MortalityCause mortalityCause;
    private String mortalityNotes;
    private Long diseaseCaseId;
    private Long siloId;
    private String barnPhotoUrl;
    private String recordedBy;
    private Double mortalityRate;
    private Long ageDays;
    private Long ageWeeks;
    private List<DailyFeedUsageLineResponse> feedLines = new ArrayList<>();
    private List<DailyMedicationLineResponse> medicationLines = new ArrayList<>();
    private List<DailyMortalityLineResponse> mortalityLines = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void setId(Long id) { this.id = id; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
    public void setFarmName(String farmName) { this.farmName = farmName; }
    public void setFlockId(Long flockId) { this.flockId = flockId; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public void setOpeningBirdCount(Integer openingBirdCount) { this.openingBirdCount = openingBirdCount; }
    public void setMortality(Integer mortality) { this.mortality = mortality; }
    public void setCulledChickens(Integer culledChickens) { this.culledChickens = culledChickens; }
    public void setFeedConsumed(BigDecimal feedConsumed) { this.feedConsumed = feedConsumed; }
    public void setWaterConsumed(BigDecimal waterConsumed) { this.waterConsumed = waterConsumed; }
    public void setAverageWeight(BigDecimal averageWeight) { this.averageWeight = averageWeight; }
    public void setSymptomsOrRemarks(String symptomsOrRemarks) { this.symptomsOrRemarks = symptomsOrRemarks; }
    public void setMortalityCause(com.trustagro.veterinary.entity.MortalityCause mortalityCause) { this.mortalityCause = mortalityCause; }
    public void setMortalityNotes(String mortalityNotes) { this.mortalityNotes = mortalityNotes; }
    public void setDiseaseCaseId(Long diseaseCaseId) { this.diseaseCaseId = diseaseCaseId; }
    public void setSiloId(Long siloId) { this.siloId = siloId; }
    public void setBarnPhotoUrl(String barnPhotoUrl) { this.barnPhotoUrl = barnPhotoUrl; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public void setMortalityRate(Double mortalityRate) { this.mortalityRate = mortalityRate; }
    public void setAgeDays(Long ageDays) { this.ageDays = ageDays; }
    public void setAgeWeeks(Long ageWeeks) { this.ageWeeks = ageWeeks; }
    public void setFeedLines(List<DailyFeedUsageLineResponse> feedLines) { this.feedLines = feedLines; }
    public void setMedicationLines(List<DailyMedicationLineResponse> medicationLines) { this.medicationLines = medicationLines; }
    public void setMortalityLines(List<DailyMortalityLineResponse> mortalityLines) { this.mortalityLines = mortalityLines; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
