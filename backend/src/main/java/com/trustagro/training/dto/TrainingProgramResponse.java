package com.trustagro.training.dto;

import com.trustagro.training.entity.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TrainingProgramResponse {
    private Long id;
    private String programCode;
    private String title;
    private String description;
    private TrainingCategory category;
    private String objectives;
    private Integer durationHours;
    private DeliveryMode deliveryMode;
    private String location;
    private String trainerName;
    private Integer defaultCapacity;
    private BigDecimal price;
    private String currency;
    private ProgramStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate registrationDeadline;
    private LocalDateTime createdAt;
    private int sessionCount;
}
