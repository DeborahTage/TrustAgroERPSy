package com.trustagro.farm.dto;

import lombok.Data;

@Data
public class DailyMortalityLineResponse {
    private Long id;
    private Integer deathCount;
    private com.trustagro.veterinary.entity.MortalityCause cause;
    private String notes;
}
