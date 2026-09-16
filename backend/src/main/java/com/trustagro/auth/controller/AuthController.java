package com.trustagro.auth.controller;

import com.trustagro.auth.dto.ChangePasswordRequest;
import com.trustagro.auth.dto.LoginRequest;
import com.trustagro.auth.dto.LoginResponse;
import com.trustagro.auth.service.AuthService;
import com.trustagro.common.response.ApiResponse;
import com.trustagro.user.dto.UserResponse;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import com.trustagro.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints. JWT logout is client-side only (no server-side token revoke).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(req)));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(userService.toResponse(user)));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(userDetails.getUsername(), req);
        return ResponseEntity.ok(ApiResponse.success("Password updated", null));
    }

    /**
     * Audit logout. JWT remains valid until expiry — clients must discard the token.
     * There is no server-side session or token blacklist.
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                "Logged out. Discard the JWT client-side; server does not revoke tokens.", null));
    }
}
