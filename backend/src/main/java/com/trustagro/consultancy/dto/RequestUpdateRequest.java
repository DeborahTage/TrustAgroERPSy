package com.trustagro.consultancy.dto;

import com.trustagro.consultancy.entity.RequestPriority;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RequestUpdateRequest {
    private String subject;
    private String description;
    private Long serviceId;
    private LocalDate requestedDate;
    private String preferredLocation;
    private RequestPriority priority;
    private String notes;
}
