package com.trustagro.consultancy.dto;

import com.trustagro.consultancy.entity.ServiceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ServiceCatalogRequest {
    private String serviceCode;
    @NotBlank
    private String name;
    private String description;
    private ServiceCategory category;
    private Integer defaultDurationDays;
    @NotNull
    private BigDecimal defaultPrice;
    private String currency;
    private Boolean taxApplicable;
    private Boolean active;
}
