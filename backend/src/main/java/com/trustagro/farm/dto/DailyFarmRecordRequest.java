package com.trustagro.farm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyFarmRecordRequest {
    @NotNull
    private LocalDate date;
    @NotNull
    private Long farmId;
    @NotNull
    private Long flockId;
    private Integer openingBirdCount;
    @Min(0) private Integer mortality;
    @Min(0) private Integer culledBirds;
    @Min(0) private BigDecimal feedConsumed;
    @Min(0) private BigDecimal waterConsumed;
    @PositiveOrZero
    private BigDecimal averageWeight;
    @Size(max = 1000)
    private String symptomsOrRemarks;
    private com.trustagro.veterinary.entity.MortalityCause mortalityCause;
    private String mortalityNotes;
    private Long diseaseCaseId;
    private Long siloId;
    private String barnPhotoUrl;

    public LocalDate getDate() { return date; }
    public Long getFarmId() { return farmId; }
    public Long getFlockId() { return flockId; }
    public Integer getOpeningBirdCount() { return openingBirdCount; }
    public Integer getMortality() { return mortality; }
    public Integer getCulledBirds() { return culledBirds; }
    public BigDecimal getFeedConsumed() { return feedConsumed; }
    public BigDecimal getWaterConsumed() { return waterConsumed; }
    public BigDecimal getAverageWeight() { return averageWeight; }
    public String getSymptomsOrRemarks() { return symptomsOrRemarks; }
    public com.trustagro.veterinary.entity.MortalityCause getMortalityCause() { return mortalityCause; }
    public String getMortalityNotes() { return mortalityNotes; }
    public Long getDiseaseCaseId() { return diseaseCaseId; }
    public Long getSiloId() { return siloId; }
    public String getBarnPhotoUrl() { return barnPhotoUrl; }
}
