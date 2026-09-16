package com.trustagro.hr.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentRequest {
    private String code;
    @NotBlank
    private String name;
    private String description;
    private Boolean active;
}
