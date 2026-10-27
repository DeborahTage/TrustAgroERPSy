package com.trustagro.hr.dto;

import com.trustagro.hr.entity.DeductionCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DeductionTypeResponse {
    private Long id;
    private String code;
    private String name;
    private DeductionCategory category;
    private BigDecimal ratePercent;
    private BigDecimal fixedAmount;
    private boolean active;
    private boolean approved;
    private String notes;
    private LocalDateTime createdAt;
}
