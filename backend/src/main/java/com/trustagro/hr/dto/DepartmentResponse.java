package com.trustagro.hr.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DepartmentResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private boolean active;
    private long employeeCount;
    private LocalDateTime createdAt;
}
