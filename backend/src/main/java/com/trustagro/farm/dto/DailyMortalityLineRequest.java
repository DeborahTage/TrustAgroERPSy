package com.trustagro.farm.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DailyMortalityLineRequest {
    @Min(0)
    private Integer deathCount;
    private com.trustagro.veterinary.entity.MortalityCause cause;
    private String notes;
}
