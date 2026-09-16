package com.trustagro.governance;

import com.trustagro.audit.entity.AuditLog;
import com.trustagro.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GovernanceService {

    private final AuditService auditService;
    private final ControlReportsService controlReportsService;

    public Map<String, Object> dashboard() {
        List<ControlCheckResult> controls = controlReportsService.runAll();
        long errorCount = controls.stream().filter(c -> ControlReportsService.ERROR.equals(c.getStatus())).count();
        long warningCount = controls.stream().filter(c -> ControlReportsService.WARNING.equals(c.getStatus())).count();

        long pendingProc = controlReportsService.countPendingProcurementApprovals();
        long pendingLeave = controlReportsService.countPendingLeaveApprovals();
        long pendingPayroll = controlReportsService.countPendingPayrollApprovals();
        long pendingApprovals = pendingProc + pendingLeave + pendingPayroll;

        List<ControlCheckResult> exceptions = controls.stream()
                .filter(c -> ControlReportsService.ERROR.equals(c.getStatus())
                        || ControlReportsService.WARNING.equals(c.getStatus())
                        || ControlReportsService.UNAVAILABLE.equals(c.getStatus()))
                .collect(Collectors.toList());

        List<AuditLog> highRisk = auditService.recentHighRisk(15);
        List<Map<String, Object>> highRiskMaps = new ArrayList<>();
        for (AuditLog a : highRisk) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("action", a.getAction());
            row.put("module", a.getModule());
            row.put("userEmail", a.getUserEmail());
            row.put("entityType", a.getEntityType());
            row.put("entityId", a.getEntityId());
            row.put("result", a.getResult());
            row.put("createdAt", a.getCreatedAt());
            highRiskMaps.add(row);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("auditToday", auditService.countToday());
        out.put("failedLogins24h", auditService.failedLoginsSince(LocalDateTime.now().minusHours(24)));
        out.put("pendingApprovals", pendingApprovals);
        out.put("pendingApprovalsDetail", Map.of(
                "procurement", pendingProc,
                "leave", pendingLeave,
                "payroll", pendingPayroll
        ));
        out.put("controlErrorCount", errorCount);
        out.put("controlWarningCount", warningCount);
        out.put("controlExceptions", exceptions);
        out.put("recentHighRisk", highRiskMaps);
        out.put("generatedAt", LocalDateTime.now());
        return out;
    }

    /** Lightweight summary for executive alerts — avoids heavy nested payloads. */
    public Map<String, Object> executiveSummary() {
        List<ControlCheckResult> controls = controlReportsService.runAll();
        long errorCount = controls.stream().filter(c -> ControlReportsService.ERROR.equals(c.getStatus())).count();
        long pending = controlReportsService.countPendingProcurementApprovals()
                + controlReportsService.countPendingLeaveApprovals()
                + controlReportsService.countPendingPayrollApprovals();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("pendingApprovals", pending);
        summary.put("controlErrorCount", errorCount);
        summary.put("failedLogins24h", auditService.failedLoginsSince(LocalDateTime.now().minusHours(24)));
        return summary;
    }
}
