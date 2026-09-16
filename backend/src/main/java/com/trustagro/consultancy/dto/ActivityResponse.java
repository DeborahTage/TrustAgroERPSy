package com.trustagro.consultancy.dto;

import com.trustagro.consultancy.entity.ActivityStatus;
import com.trustagro.consultancy.entity.ActivityType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class ActivityResponse {
    private Long id;
    private String activityNumber;
    private Long engagementId;
    private String engagementNumber;
    private Long customerId;
    private Long consultantId;
    private String consultantName;
    private ActivityType activityType;
    private LocalDate activityDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private String location;
    private String description;
    private String findings;
    private String recommendations;
    private String customerNotes;
    private ActivityStatus status;
    private LocalDateTime createdAt;
}
