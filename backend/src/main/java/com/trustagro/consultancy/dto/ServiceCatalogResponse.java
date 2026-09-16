package com.trustagro.consultancy.dto;

import com.trustagro.consultancy.entity.ServiceCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ServiceCatalogResponse {
    private Long id;
    private String serviceCode;
    private String name;
    private String description;
    private ServiceCategory category;
    private Integer defaultDurationDays;
    private BigDecimal defaultPrice;
    private String currency;
    private boolean taxApplicable;
    private boolean active;
    private LocalDateTime createdAt;
}
