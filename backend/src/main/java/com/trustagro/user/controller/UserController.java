package com.trustagro.user.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.user.dto.UserRequest;
import com.trustagro.user.dto.UserResponse;
import com.trustagro.user.entity.UserStatus;
import com.trustagro.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('users:read')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('users:read')")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('users:create')")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserRequest req) {
        return ResponseEntity.ok(ApiResponse.success("User created", userService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('users:update')")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id, @Valid @RequestBody UserRequest req) {
        return ResponseEntity.ok(ApiResponse.success("User updated", userService.update(id, req)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('users:update')")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        UserStatus status = UserStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(ApiResponse.success("Status updated", userService.updateStatus(id, status)));
    }
}
