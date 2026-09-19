package com.trustagro.materials.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FinanceDecisionRequest {
    private BigDecimal approvedAmount;
    private String reason;
    private String notes;
}
