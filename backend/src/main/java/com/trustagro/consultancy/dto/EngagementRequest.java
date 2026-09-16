package com.trustagro.consultancy.dto;

import com.trustagro.consultancy.entity.BillingType;
import com.trustagro.consultancy.entity.RequestPriority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EngagementRequest {
    @NotNull
    private Long customerId;
    private Long serviceId;
    private Long proposalId;
    private Long requestId;
    @NotBlank
    private String title;
    private String description;
    private String objectives;
    private String scope;
    private LocalDate startDate;
    private LocalDate expectedEndDate;
    private String location;
    private RequestPriority priority;
    private Long leadConsultantId;
    private BigDecimal estimatedAmount;
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal agreedAmount;
    private String currency;
    private BillingType billingType;
    private String notes;
}
