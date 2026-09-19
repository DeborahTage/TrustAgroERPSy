package com.trustagro.veterinary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trustagro.veterinary.entity.DiseaseStatus;
import com.trustagro.veterinary.entity.DiseaseSeverity;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DiseaseCaseResponse {
    private Long id;
    private Long farmId;
    private String farmName;
    private Long flockId;
    private String batchCode;
    private LocalDate dateDetected;
    private String symptoms;
    private String suspectedDisease;
    private Integer numberAffected;
    private Integer numberDead;
    private DiseaseSeverity severity;
    private DiseaseStatus status;
    private String actionTaken;
    private String reportedBy;
    private String attachmentUrl;
    private LocalDateTime createdAt;

    public void setId(Long id) { this.id = id; }
    public void setFarmId(Long farmId) { this.farmId = farmId; }
    public void setFarmName(String farmName) { this.farmName = farmName; }
    public void setFlockId(Long flockId) { this.flockId = flockId; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public void setDateDetected(LocalDate dateDetected) { this.dateDetected = dateDetected; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    public void setSuspectedDisease(String suspectedDisease) { this.suspectedDisease = suspectedDisease; }
    public void setNumberAffected(Integer numberAffected) { this.numberAffected = numberAffected; }
    public void setNumberDead(Integer numberDead) { this.numberDead = numberDead; }
    public void setSeverity(DiseaseSeverity severity) { this.severity = severity; }
    public void setStatus(DiseaseStatus status) { this.status = status; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @JsonProperty("diseaseName")
    public String getDiseaseName() {
        return suspectedDisease;
    }

    @JsonProperty("detectedDate")
    public LocalDate getDetectedDate() {
        return dateDetected;
    }

    @JsonProperty("affectedChickens")
    public Integer getAffectedChickens() {
        return numberAffected;
    }

    @JsonProperty("deadChickens")
    public Integer getDeadChickens() {
        return numberDead;
    }
}
