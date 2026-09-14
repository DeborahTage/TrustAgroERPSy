package com.trustagro.notification.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.notification.dto.NotificationResponse;
import com.trustagro.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAuthority('notifications:read')")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAll(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getAll(userDetails.getUsername())));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAuthority('notifications:read')")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.markRead(id, userDetails.getUsername())));
    }
}
