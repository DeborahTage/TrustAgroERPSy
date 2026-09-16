package com.trustagro.hr.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LeaveTypeResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private boolean balanceControlled;
    private BigDecimal defaultAnnualEntitlement;
    private boolean paid;
    private boolean active;
    private LocalDateTime createdAt;
}
