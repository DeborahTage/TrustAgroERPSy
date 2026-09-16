package com.trustagro.executive;

import com.trustagro.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/executive")
@RequiredArgsConstructor
public class ExecutiveController {

    private final ExecutiveService executiveService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('executive:dashboard','users:read','reports:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String businessLine) {
        return ResponseEntity.ok(ApiResponse.success(
                executiveService.dashboard(preset, from, to, businessLine)));
    }

    @GetMapping("/financial-summary")
    @PreAuthorize("hasAnyAuthority('executive:finance','finance:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> financialSummary(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String businessLine) {
        return ResponseEntity.ok(ApiResponse.success(
                executiveService.financialSummary(preset, from, to, businessLine)));
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyAuthority('executive:finance','finance:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> revenue(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String businessLine) {
        return ResponseEntity.ok(ApiResponse.success(
                executiveService.revenue(preset, from, to, businessLine)));
    }

    @GetMapping("/expenses")
    @PreAuthorize("hasAnyAuthority('executive:finance','finance:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> expenses(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String businessLine) {
        return ResponseEntity.ok(ApiResponse.success(
                executiveService.expenses(preset, from, to, businessLine)));
    }

    @GetMapping("/cash-flow")
    @PreAuthorize("hasAnyAuthority('executive:finance','finance:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cashFlow(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                executiveService.cashFlow(preset, from, to)));
    }

    @GetMapping("/receivables")
    @PreAuthorize("hasAnyAuthority('executive:finance','finance:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> receivables(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                executiveService.receivables(preset, from, to)));
    }

    @GetMapping("/payables")
    @PreAuthorize("hasAnyAuthority('executive:finance','finance:read','procurement:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> payables(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                executiveService.payables(preset, from, to)));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAnyAuthority('executive:sales','crm:read','crm:report')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sales(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String businessLine) {
        return ResponseEntity.ok(ApiResponse.success(
                executiveService.sales(preset, from, to, businessLine)));
    }

    @GetMapping("/farm")
    @PreAuthorize("hasAnyAuthority('executive:farm','farm:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> farm(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                executiveService.farm(preset, from, to)));
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyAuthority('executive:inventory','inventory:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> inventory() {
        return ResponseEntity.ok(ApiResponse.success(executiveService.inventory()));
    }

    @GetMapping("/procurement")
    @PreAuthorize("hasAnyAuthority('procurement:read','executive:finance','executive:inventory')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> procurement() {
        return ResponseEntity.ok(ApiResponse.success(executiveService.procurement()));
    }

    @GetMapping("/services")
    @PreAuthorize("hasAnyAuthority('consultancy:read','training:read','executive:sales')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> services() {
        return ResponseEntity.ok(ApiResponse.success(executiveService.services()));
    }

    @GetMapping("/workforce")
    @PreAuthorize("hasAnyAuthority('executive:hr','hr:report','hr:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> workforce() {
        return ResponseEntity.ok(ApiResponse.success(executiveService.workforce()));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyAuthority('executive:dashboard','users:read','reports:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> alerts(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(
                executiveService.alerts(preset, from, to)));
    }

    @GetMapping("/definitions")
    @PreAuthorize("hasAnyAuthority('executive:dashboard','users:read','reports:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> definitions() {
        return ResponseEntity.ok(ApiResponse.success(executiveService.definitions()));
    }
}
