package com.trustagro.inventory.controller;

import com.trustagro.inventory.service.InventoryReportService;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory/reports")
public class InventoryReportController {

    private final InventoryReportService reportService;
    private final UserRepository userRepository;

    public InventoryReportController(InventoryReportService reportService, UserRepository userRepository) {
        this.reportService = reportService;
        this.userRepository = userRepository;
    }

    private User getUser(Authentication auth) {
        if (auth == null) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    @GetMapping("/valuation")
    @PreAuthorize("hasAnyAuthority('inventory:read','reports:read')")
    public ResponseEntity<Map<String, Object>> getValuationReport() {
        return ResponseEntity.ok(reportService.getStockValuationReport());
    }

    @GetMapping("/valuation/csv")
    @PreAuthorize("hasAnyAuthority('inventory:read','reports:read')")
    public ResponseEntity<byte[]> getValuationCsv() {
        String csv = reportService.generateValuationCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stock_valuation.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/expiry/csv")
    @PreAuthorize("hasAnyAuthority('inventory:read','reports:read')")
    public ResponseEntity<byte[]> getExpiryCsv() {
        String csv = reportService.generateExpiryCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expiry_report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/turnover")
    @PreAuthorize("hasAnyAuthority('inventory:read','reports:read')")
    public ResponseEntity<java.util.List<Map<String, Object>>> getTurnover() {
        return ResponseEntity.ok(reportService.getTurnoverReport());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('inventory:read','reports:read')")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(reportService.getInventorySummary());
    }
}
