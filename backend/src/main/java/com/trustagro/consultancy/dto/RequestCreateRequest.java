package com.trustagro.consultancy.dto;

import com.trustagro.consultancy.entity.RequestPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RequestCreateRequest {
    @NotNull
    private Long customerId;
    @NotBlank
    private String subject;
    private String description;
    private Long serviceId;
    private LocalDate requestedDate;
    private String preferredLocation;
    private RequestPriority priority;
    private String notes;
}
