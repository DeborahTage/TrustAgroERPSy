package com.trustagro.farm.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ChickenDispatchRequest {
    private Long customerId;
    @NotNull
    private LocalDate dispatchDate;
    @Size(max = 80)
    private String chickenType;
    private Integer bonusQuantity;
    private Long flockId;
    private String notes;
}
