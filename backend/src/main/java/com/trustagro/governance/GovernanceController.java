package com.trustagro.governance;

import com.trustagro.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/governance")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('audit:read','executive:dashboard','reports:read','governance:read')")
public class GovernanceController {

    private final GovernanceService governanceService;
    private final ControlReportsService controlReportsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('audit:read','executive:dashboard','governance:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(governanceService.dashboard()));
    }

    @GetMapping("/controls")
    @PreAuthorize("hasAnyAuthority('audit:read','reports:read','governance:read','executive:dashboard')")
    public ResponseEntity<ApiResponse<List<ControlCheckResult>>> controls() {
        return ResponseEntity.ok(ApiResponse.success(controlReportsService.runAll()));
    }
}
