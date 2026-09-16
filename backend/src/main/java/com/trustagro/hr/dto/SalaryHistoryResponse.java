package com.trustagro.hr.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SalaryHistoryResponse {
    private Long id;
    private Long employeeId;
    private String employeeNumber;
    private LocalDate effectiveDate;
    private BigDecimal basicSalary;
    private String currency;
    private String reason;
    private String approvedByName;
    private LocalDateTime createdAt;
}
