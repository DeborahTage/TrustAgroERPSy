package com.trustagro.audit.controller;

import com.trustagro.audit.entity.AuditLog;
import com.trustagro.audit.service.AuditService;
import com.trustagro.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('audit:read')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAll(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        // If no filters are provided, use the full query path
        if (module == null && entityType == null && action == null && userId == null
                && startDate == null && endDate == null) {
            return ResponseEntity.ok(ApiResponse.success(auditService.getAll()));
        }
        List<AuditLog> logs = auditService.getWithFilters(module, entityType, action, userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
