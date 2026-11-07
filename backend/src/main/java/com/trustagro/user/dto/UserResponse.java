package com.trustagro.user.dto;

import com.trustagro.user.entity.OrganizationalDesignation;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private OrganizationalDesignation designation;
    private Long reportsToId;
    private String reportsToName;
    private Boolean isGroupFarmManager;
    private Boolean isSiteFarmManager;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> permissions;
    private List<Long> farmIds;
    private Boolean mustChangePassword;
}
