package com.trustagro.consultancy.dto;

import com.trustagro.consultancy.entity.ActivityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ActivityRequest {
    @NotNull
    private Long engagementId;
    private Long consultantId;
    private ActivityType activityType;
    @NotNull
    private LocalDate activityDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String location;
    @NotBlank
    private String description;
    private String findings;
    private String recommendations;
    private String customerNotes;
}
