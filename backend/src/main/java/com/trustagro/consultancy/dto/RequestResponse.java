package com.trustagro.consultancy.dto;

import com.trustagro.consultancy.entity.RequestPriority;
import com.trustagro.consultancy.entity.RequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class RequestResponse {
    private Long id;
    private String requestNumber;
    private Long customerId;
    private String customerName;
    private String subject;
    private String description;
    private Long serviceId;
    private String serviceName;
    private LocalDate requestedDate;
    private String preferredLocation;
    private RequestPriority priority;
    private Long assignedConsultantId;
    private String assignedConsultantName;
    private RequestStatus status;
    private String notes;
    private LocalDateTime createdAt;
}
