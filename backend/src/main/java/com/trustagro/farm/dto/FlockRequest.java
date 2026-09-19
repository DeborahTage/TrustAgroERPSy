package com.trustagro.farm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FlockRequest {
    @NotBlank
    @Size(max = 60)
    private String batchCode;
    @NotNull
    private Long farmId;
    @Size(max = 80)
    private String birdType;
    @Positive
    private Integer initialBirdCount;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private String breed;
    private Long supplierId;
    private Long responsiblePersonId;
    private String notes;
    private String sourceDocumentUrl;

    public Long getFarmId() { return farmId; }
    public String getBatchCode() { return batchCode; }
    public String getBirdType() { return birdType; }
    public String getBreed() { return breed; }
    public Integer getInitialBirdCount() { return initialBirdCount; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getExpectedEndDate() { return expectedEndDate; }
    public Long getSupplierId() { return supplierId; }
    public Long getResponsiblePersonId() { return responsiblePersonId; }
    public String getNotes() { return notes; }
    public String getSourceDocumentUrl() { return sourceDocumentUrl; }
}
