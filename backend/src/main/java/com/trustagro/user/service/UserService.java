package com.trustagro.user.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.security.RoleCompatibility;
import com.trustagro.security.RolePermissionCatalog;
import com.trustagro.user.dto.UserRequest;
import com.trustagro.user.dto.UserResponse;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.trustagro.audit.service.AuditService auditService;

    private static final String MODULE = "USER";

    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public UserResponse getById(Long id) {
        return toResponse(findById(id));
    }

    public UserResponse create(UserRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new BusinessException("Email already in use");
        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(RoleCompatibility.normalize(req.getRole()));
        User saved = userRepository.save(user);
        auditService.logObject("CREATE", MODULE, "USER", saved.getId(), null, toResponse(saved));
        return toResponse(saved);
    }

    public UserResponse update(Long id, UserRequest req) {
        User user = findById(id);
        UserResponse before = toResponse(user);
        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        user.setRole(RoleCompatibility.normalize(req.getRole()));
        if (req.getPassword() != null && !req.getPassword().isBlank())
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        User saved = userRepository.save(user);
        auditService.logObject("UPDATE", MODULE, "USER", saved.getId(), before, toResponse(saved));
        return toResponse(saved);
    }

    public UserResponse updateStatus(Long id, UserStatus status) {
        User user = findById(id);
        UserResponse before = toResponse(user);
        user.setStatus(status);
        User saved = userRepository.save(user);
        auditService.logObject("TOGGLE_STATUS", MODULE, "USER", saved.getId(), before, toResponse(saved));
        return toResponse(saved);
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public UserResponse toResponse(User user) {
        Role canonical = RoleCompatibility.normalize(user.getRole());
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setFullName(user.getFullName());
        r.setEmail(user.getEmail());
        r.setPhone(user.getPhone());
        r.setRole(canonical);
        r.setStatus(user.getStatus());
        r.setCreatedAt(user.getCreatedAt());
        r.setUpdatedAt(user.getUpdatedAt());
        r.setPermissions(RolePermissionCatalog.permissionsFor(canonical));
        r.setFarmIds(user.getFarmIds() != null ? user.getFarmIds() : List.of());
        return r;
    }
}
