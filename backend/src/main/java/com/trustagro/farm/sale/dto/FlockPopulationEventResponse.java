package com.trustagro.farm.sale.dto;

import com.trustagro.farm.sale.entity.FlockPopulationEventType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FlockPopulationEventResponse {
    private Long id;
    private FlockPopulationEventType eventType;
    private Integer previousPopulation;
    private Integer quantityChange;
    private Integer newPopulation;
    private Long chickenSaleId;
    private LocalDate eventDate;
    private String recordedByName;
    private String notes;
    private LocalDateTime createdAt;
}
