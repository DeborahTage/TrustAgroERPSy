package com.trustagro.auth.service;

import com.trustagro.auth.dto.LoginRequest;
import com.trustagro.auth.dto.LoginResponse;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.RoleEntity;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.RoleRepository;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException("User not found"));
        if (user.getStatus() == UserStatus.INACTIVE)
            throw new BusinessException("Account is inactive");

        // Resolve permissions from role
        List<String> permissions = resolvePermissions(user.getRole());
        List<Long> farmIds = user.getFarmIds() != null ? user.getFarmIds() : List.of();

        String token = jwtService.generateToken(user.getEmail(), permissions, farmIds);
        return new LoginResponse(token, user.getId(), user.getFullName(), user.getEmail(),
                user.getRole(), permissions, farmIds);
    }

    private List<String> resolvePermissions(Role role) {
        return roleRepository.findByName(role.name())
                .map(RoleEntity::getPermissions)
                .orElseGet(() -> role == Role.ADMIN ? List.of("*") : List.of());
    }
}
