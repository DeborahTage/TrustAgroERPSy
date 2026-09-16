package com.trustagro.consultancy.dto;

import com.trustagro.consultancy.entity.ContractStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ContractResponse {
    private Long id;
    private String contractNumber;
    private Long engagementId;
    private String engagementNumber;
    private Long customerId;
    private String customerName;
    private String title;
    private String scope;
    private String terms;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal contractValue;
    private String currency;
    private ContractStatus status;
    private LocalDate signedDate;
    private String notes;
    private LocalDateTime createdAt;
}
