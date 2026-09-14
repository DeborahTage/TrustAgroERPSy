package com.trustagro.training.dto;

import com.trustagro.training.entity.DeliveryMode;
import com.trustagro.training.entity.TrainingCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TrainingProgramRequest {
    private String programCode;
    @NotBlank
    private String title;
    private String description;
    private TrainingCategory category;
    private String objectives;
    private Integer durationHours;
    private DeliveryMode deliveryMode;
    private String location;
    private String trainerName;
    private Integer defaultCapacity;
    @NotNull
    private BigDecimal price;
    private String currency;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registrationDeadline;
}
