package com.trustagro.user.dto;

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
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> permissions;
    private List<Long> farmIds;
}
