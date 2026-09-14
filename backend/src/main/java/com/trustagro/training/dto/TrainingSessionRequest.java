package com.trustagro.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TrainingSessionRequest {
    @NotNull
    private Long programId;
    private String sessionCode;
    @NotNull
    private LocalDate startDate;
    private LocalDate endDate;
    private String venue;
    @NotNull
    private Integer capacity;
    private String trainerName;
    private String notes;
}
