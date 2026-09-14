package com.trustagro.training.dto;

import com.trustagro.training.entity.SessionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TrainingSessionResponse {
    private Long id;
    private Long programId;
    private String programCode;
    private String programTitle;
    private String sessionCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private String venue;
    private Integer capacity;
    private Integer confirmedCount;
    private Integer remainingCapacity;
    private String trainerName;
    private SessionStatus status;
    private String notes;
    private LocalDateTime createdAt;
}
