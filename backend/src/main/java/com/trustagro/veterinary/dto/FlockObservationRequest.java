package com.trustagro.veterinary.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FlockObservationRequest {
    private Long flockId;
    private Integer respiratoryDistressScore;
    private String diarrheaType;
    private Double feedIntakeDropPercentage;
    private Double waterIntakeDropPercentage;
    private Integer mortalityCount;
    private String generalComments;

    public Long getFlockId() { return flockId; }
    public String getGeneralComments() { return generalComments; }
    public Integer getRespiratoryDistressScore() { return respiratoryDistressScore; }
    public String getDiarrheaType() { return diarrheaType; }
    public Double getFeedIntakeDropPercentage() { return feedIntakeDropPercentage; }
    public Double getWaterIntakeDropPercentage() { return waterIntakeDropPercentage; }
    public Integer getMortalityCount() { return mortalityCount; }
}
