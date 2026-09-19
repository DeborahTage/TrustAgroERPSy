package com.trustagro.veterinary.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.trustagro.veterinary.entity.DiseaseSeverity;
import com.trustagro.veterinary.entity.DiseaseStatus;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DiseaseCaseRequest {
    @NotNull private Long farmId;
    @NotNull private Long flockId;
    @JsonAlias("detectedDate")
    private LocalDate dateDetected;
    @Size(max = 1000)
    private String symptoms;
    @JsonAlias("diseaseName")
    @Size(max = 120)
    private String suspectedDisease;
    @JsonAlias("affectedChickens")
    @PositiveOrZero
    private Integer numberAffected;
    @JsonAlias("deadChickens")
    @PositiveOrZero
    private Integer numberDead;
    private DiseaseSeverity severity;
    private DiseaseStatus status;
    @Size(max = 2000)
    private String actionTaken;
    private String attachmentUrl;

    public Long getFarmId() { return farmId; }
    public Long getFlockId() { return flockId; }
    public LocalDate getDateDetected() { return dateDetected; }
    public String getSymptoms() { return symptoms; }
    public String getSuspectedDisease() { return suspectedDisease; }
    public Integer getNumberAffected() { return numberAffected; }
    public Integer getNumberDead() { return numberDead; }
    public DiseaseSeverity getSeverity() { return severity; }
    public DiseaseStatus getStatus() { return status; }
    public String getActionTaken() { return actionTaken; }
    public String getAttachmentUrl() { return attachmentUrl; }
}
