package com.trustagro.farm.dto;

import com.trustagro.user.entity.OrganizationalDesignation;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SiteFarmManagerResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private OrganizationalDesignation designation;
    private UserStatus status;
    private Long reportsToId;
    private String reportsToName;
    private Long assignedFarmId;
    private String assignedFarmName;
    private String assignedFarmLocation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
