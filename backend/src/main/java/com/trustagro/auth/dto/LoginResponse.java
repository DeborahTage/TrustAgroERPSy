package com.trustagro.auth.dto;

import com.trustagro.user.entity.Role;
import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {
    private String token;
    private Long userId;
    private String fullName;
    private String email;
    private Role role;
    private List<String> permissions;
    private List<Long> farmIds;
    private Boolean mustChangePassword = false;

    public LoginResponse() {}

    public LoginResponse(String token, Long userId, String fullName, String email, Role role,
                         List<String> permissions, List<Long> farmIds) {
        this(token, userId, fullName, email, role, permissions, farmIds, false);
    }

    public LoginResponse(String token, Long userId, String fullName, String email, Role role,
                         List<String> permissions, List<Long> farmIds, Boolean mustChangePassword) {
        this.token = token;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.permissions = permissions;
        this.farmIds = farmIds;
        this.mustChangePassword = mustChangePassword != null && mustChangePassword;
    }
}
