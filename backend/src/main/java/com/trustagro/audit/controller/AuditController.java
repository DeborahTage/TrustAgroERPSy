package com.trustagro.audit.controller;

import com.trustagro.audit.entity.AuditLog;
import com.trustagro.audit.service.AuditService;
import com.trustagro.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('audit:read')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String keyword
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AuditLog> result = auditService.search(
                blankToNull(module), blankToNull(action), blankToNull(entityType),
                entityId, userId, startDate, endDate, blankToNull(keyword),
                PageRequest.of(Math.max(page, 0), safeSize));
        return ResponseEntity.ok(ApiResponse.success(toPageMap(result)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLog>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getById(id)));
    }

    @GetMapping("/security-events")
    public ResponseEntity<ApiResponse<Map<String, Object>>> securityEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AuditLog> result = auditService.securityEvents(
                startDate, endDate, PageRequest.of(Math.max(page, 0), safeSize));
        return ResponseEntity.ok(ApiResponse.success(toPageMap(result)));
    }

    private static Map<String, Object> toPageMap(Page<AuditLog> page) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("content", page.getContent());
        map.put("page", page.getNumber());
        map.put("size", page.getSize());
        map.put("totalElements", page.getTotalElements());
        map.put("totalPages", page.getTotalPages());
        map.put("first", page.isFirst());
        map.put("last", page.isLast());
        return map;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
