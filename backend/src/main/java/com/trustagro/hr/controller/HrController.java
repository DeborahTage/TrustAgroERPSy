package com.trustagro.hr.controller;

import com.trustagro.common.response.ApiResponse;
import com.trustagro.hr.dto.*;
import com.trustagro.hr.entity.EmploymentStatus;
import com.trustagro.hr.entity.LeaveRequestStatus;
import com.trustagro.hr.entity.PayrollPeriodStatus;
import com.trustagro.hr.service.HrService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
public class HrController {

    private final HrService hrService;

    // ─── Dashboard & reports ─────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('hr:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success(hrService.dashboard()));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyAuthority('hr:report','hr:read','reports:read')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reports() {
        return ResponseEntity.ok(ApiResponse.success(hrService.reports()));
    }

    // ─── Departments ─────────────────────────────────────────────

    @GetMapping("/departments")
    @PreAuthorize("hasAuthority('hr:read')")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> listDepartments(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(hrService.listDepartments(activeOnly)));
    }

    @GetMapping("/departments/{id}")
    @PreAuthorize("hasAuthority('hr:read')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.getDepartment(id)));
    }

    @PostMapping("/departments")
    @PreAuthorize("hasAuthority('hr:create')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody DepartmentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.createDepartment(req)));
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasAuthority('hr:update')")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable Long id, @RequestBody DepartmentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.updateDepartment(id, req)));
    }

    // ─── Positions ───────────────────────────────────────────────

    @GetMapping("/positions")
    @PreAuthorize("hasAuthority('hr:read')")
    public ResponseEntity<ApiResponse<List<PositionResponse>>> listPositions(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(hrService.listPositions(activeOnly)));
    }

    @GetMapping("/positions/{id}")
    @PreAuthorize("hasAuthority('hr:read')")
    public ResponseEntity<ApiResponse<PositionResponse>> getPosition(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.getPosition(id)));
    }

    @PostMapping("/positions")
    @PreAuthorize("hasAuthority('hr:create')")
    public ResponseEntity<ApiResponse<PositionResponse>> createPosition(
            @Valid @RequestBody PositionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.createPosition(req)));
    }

    @PutMapping("/positions/{id}")
    @PreAuthorize("hasAuthority('hr:update')")
    public ResponseEntity<ApiResponse<PositionResponse>> updatePosition(
            @PathVariable Long id, @RequestBody PositionRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.updatePosition(id, req)));
    }

    // ─── Employees ───────────────────────────────────────────────

    @GetMapping("/employees")
    @PreAuthorize("hasAuthority('hr:read')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> listEmployees(
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(hrService.listEmployees(status, departmentId, search)));
    }

    @GetMapping("/employees/{id}")
    @PreAuthorize("hasAuthority('hr:read')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.getEmployee(id)));
    }

    @PostMapping("/employees")
    @PreAuthorize("hasAuthority('hr:create')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(
            @Valid @RequestBody EmployeeRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.createEmployee(req)));
    }

    @PutMapping("/employees/{id}")
    @PreAuthorize("hasAuthority('hr:update')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable Long id, @RequestBody EmployeeRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.updateEmployee(id, req)));
    }

    @PostMapping("/employees/{id}/status")
    @PreAuthorize("hasAnyAuthority('hr:update','hr:terminate')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> changeStatus(
            @PathVariable Long id, @Valid @RequestBody EmployeeStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.changeEmployeeStatus(id, req)));
    }

    @PostMapping("/employees/{id}/terminate")
    @PreAuthorize("hasAuthority('hr:terminate')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> terminate(
            @PathVariable Long id, @Valid @RequestBody EmployeeStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.changeEmployeeStatus(id, req)));
    }

    @GetMapping("/employees/{id}/salary")
    @PreAuthorize("hasAuthority('hr:payroll')")
    public ResponseEntity<ApiResponse<List<SalaryHistoryResponse>>> salaryHistory(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.listSalaryHistory(id)));
    }

    @PostMapping("/employees/{id}/salary")
    @PreAuthorize("hasAuthority('hr:payroll')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> changeSalary(
            @PathVariable Long id, @Valid @RequestBody SalaryChangeRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.changeSalary(id, req)));
    }

    // ─── Attendance ──────────────────────────────────────────────

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyAuthority('hr:read','hr:attendance')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> listAttendance(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(hrService.listAttendance(employeeId, from, to)));
    }

    @GetMapping("/attendance/{id}")
    @PreAuthorize("hasAnyAuthority('hr:read','hr:attendance')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getAttendance(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.getAttendance(id)));
    }

    @PostMapping("/attendance")
    @PreAuthorize("hasAuthority('hr:attendance')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> recordAttendance(
            @Valid @RequestBody AttendanceRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.recordAttendance(req)));
    }

    @PutMapping("/attendance/{id}")
    @PreAuthorize("hasAuthority('hr:attendance')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> updateAttendance(
            @PathVariable Long id, @RequestBody AttendanceRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.updateAttendance(id, req)));
    }

    // ─── Leave types ─────────────────────────────────────────────

    @GetMapping("/leave-types")
    @PreAuthorize("hasAnyAuthority('hr:read','hr:leave','hr:self')")
    public ResponseEntity<ApiResponse<List<LeaveTypeResponse>>> listLeaveTypes(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(hrService.listLeaveTypes(activeOnly)));
    }

    @PostMapping("/leave-types")
    @PreAuthorize("hasAuthority('hr:create')")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> createLeaveType(
            @Valid @RequestBody LeaveTypeRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.createLeaveType(req)));
    }

    @PutMapping("/leave-types/{id}")
    @PreAuthorize("hasAuthority('hr:update')")
    public ResponseEntity<ApiResponse<LeaveTypeResponse>> updateLeaveType(
            @PathVariable Long id, @RequestBody LeaveTypeRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.updateLeaveType(id, req)));
    }

    // ─── Leave requests ──────────────────────────────────────────

    @GetMapping("/leave-requests")
    @PreAuthorize("hasAnyAuthority('hr:read','hr:leave','hr:leave_approve')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> listLeaveRequests(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LeaveRequestStatus status) {
        return ResponseEntity.ok(ApiResponse.success(hrService.listLeaveRequests(employeeId, status)));
    }

    @GetMapping("/leave-requests/{id}")
    @PreAuthorize("hasAnyAuthority('hr:read','hr:leave','hr:leave_approve')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> getLeaveRequest(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.getLeaveRequest(id)));
    }

    @PostMapping("/leave-requests")
    @PreAuthorize("hasAuthority('hr:leave')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> createLeaveRequest(
            @Valid @RequestBody LeaveRequestCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.createLeaveRequest(req, false)));
    }

    @PostMapping("/leave-requests/{id}/approve")
    @PreAuthorize("hasAuthority('hr:leave_approve')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> approveLeave(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.approveLeaveRequest(id)));
    }

    @PostMapping("/leave-requests/{id}/reject")
    @PreAuthorize("hasAuthority('hr:leave_approve')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> rejectLeave(
            @PathVariable Long id, @RequestBody(required = false) LeaveDecisionRequest body) {
        return ResponseEntity.ok(ApiResponse.success(hrService.rejectLeaveRequest(id, body)));
    }

    @PostMapping("/leave-requests/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('hr:leave','hr:leave_approve','hr:self')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> cancelLeave(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.cancelLeaveRequest(id)));
    }

    @GetMapping("/employees/{id}/leave-balances")
    @PreAuthorize("hasAnyAuthority('hr:read','hr:leave','hr:leave_approve')")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> leaveBalances(
            @PathVariable Long id, @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.success(hrService.getLeaveBalances(id, year)));
    }

    // ─── Deduction types ─────────────────────────────────────────

    @GetMapping("/payroll/deduction-types")
    @PreAuthorize("hasAnyAuthority('hr:read','hr:payroll')")
    public ResponseEntity<ApiResponse<List<DeductionTypeResponse>>> listDeductionTypes(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.success(hrService.listDeductionTypes(activeOnly)));
    }

    @PostMapping("/payroll/deduction-types")
    @PreAuthorize("hasAuthority('hr:payroll')")
    public ResponseEntity<ApiResponse<DeductionTypeResponse>> createDeductionType(
            @Valid @RequestBody DeductionTypeRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.createDeductionType(req)));
    }

    @PutMapping("/payroll/deduction-types/{id}")
    @PreAuthorize("hasAuthority('hr:payroll')")
    public ResponseEntity<ApiResponse<DeductionTypeResponse>> updateDeductionType(
            @PathVariable Long id, @RequestBody DeductionTypeRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.updateDeductionType(id, req)));
    }

    // ─── Payroll periods ─────────────────────────────────────────

    @GetMapping("/payroll/periods")
    @PreAuthorize("hasAnyAuthority('hr:read','hr:payroll')")
    public ResponseEntity<ApiResponse<List<PayrollPeriodResponse>>> listPeriods(
            @RequestParam(required = false) PayrollPeriodStatus status) {
        return ResponseEntity.ok(ApiResponse.success(hrService.listPayrollPeriods(status)));
    }

    @GetMapping("/payroll/periods/{id}")
    @PreAuthorize("hasAnyAuthority('hr:read','hr:payroll')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> getPeriod(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.getPayrollPeriod(id)));
    }

    @PostMapping("/payroll/periods")
    @PreAuthorize("hasAuthority('hr:payroll')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> createPeriod(
            @Valid @RequestBody PayrollPeriodRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.createPayrollPeriod(req)));
    }

    @PostMapping("/payroll/periods/{id}/calculate")
    @PreAuthorize("hasAuthority('hr:payroll')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> calculate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.calculatePayroll(id)));
    }

    @PostMapping("/payroll/periods/{id}/approve")
    @PreAuthorize("hasAuthority('hr:payroll_approve')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.approvePayroll(id)));
    }

    @PostMapping("/payroll/periods/{id}/process")
    @PreAuthorize("hasAuthority('hr:payroll_process')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> process(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.processPayroll(id)));
    }

    @PostMapping("/payroll/periods/{id}/close")
    @PreAuthorize("hasAuthority('hr:payroll')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> close(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.closePayroll(id)));
    }

    @PostMapping("/payroll/periods/{id}/cancel")
    @PreAuthorize("hasAuthority('hr:payroll')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.cancelPayrollPeriod(id)));
    }

    // ─── Runs, items, payslips, payments ─────────────────────────

    @GetMapping("/payroll/runs")
    @PreAuthorize("hasAnyAuthority('hr:read','hr:payroll')")
    public ResponseEntity<ApiResponse<List<PayrollRunResponse>>> listRuns(
            @RequestParam(required = false) Long periodId) {
        return ResponseEntity.ok(ApiResponse.success(hrService.listPayrollRuns(periodId)));
    }

    @GetMapping("/payroll/runs/{id}")
    @PreAuthorize("hasAnyAuthority('hr:read','hr:payroll')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> getRun(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.getPayrollRun(id)));
    }

    @GetMapping("/payroll/runs/{id}/items")
    @PreAuthorize("hasAuthority('hr:payroll')")
    public ResponseEntity<ApiResponse<List<PayrollItemResponse>>> listRunItems(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.listPayrollItems(id)));
    }

    @GetMapping("/payroll/payslips/{id}")
    @PreAuthorize("hasAnyAuthority('hr:read','hr:payroll','hr:self')")
    public ResponseEntity<ApiResponse<PayslipResponse>> getPayslip(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.getPayslip(id)));
    }

    @GetMapping("/payroll/runs/{id}/payments")
    @PreAuthorize("hasAnyAuthority('hr:payroll','finance:read')")
    public ResponseEntity<ApiResponse<List<PayrollPaymentResponse>>> listPayments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hrService.listPayrollPayments(id)));
    }

    @PostMapping("/payroll/runs/{id}/payments")
    @PreAuthorize("hasAuthority('hr:payroll_process')")
    public ResponseEntity<ApiResponse<PayrollPaymentResponse>> pay(
            @PathVariable Long id, @Valid @RequestBody PayrollPaymentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.payPayroll(id, req)));
    }

    // ─── Self-service ────────────────────────────────────────────

    @GetMapping("/me/profile")
    @PreAuthorize("hasAuthority('hr:self')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> myProfile() {
        return ResponseEntity.ok(ApiResponse.success(hrService.myProfile()));
    }

    @GetMapping("/me/attendance")
    @PreAuthorize("hasAuthority('hr:self')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> myAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success(hrService.myAttendance(from, to)));
    }

    @GetMapping("/me/leave")
    @PreAuthorize("hasAuthority('hr:self')")
    public ResponseEntity<ApiResponse<List<LeaveRequestResponse>>> myLeave() {
        return ResponseEntity.ok(ApiResponse.success(hrService.myLeaveRequests()));
    }

    @PostMapping("/me/leave")
    @PreAuthorize("hasAuthority('hr:self')")
    public ResponseEntity<ApiResponse<LeaveRequestResponse>> submitMyLeave(
            @Valid @RequestBody LeaveRequestCreateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(hrService.createLeaveRequest(req, true)));
    }

    @GetMapping("/me/leave-balances")
    @PreAuthorize("hasAuthority('hr:self')")
    public ResponseEntity<ApiResponse<List<LeaveBalanceResponse>>> myLeaveBalances(
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ApiResponse.success(hrService.myLeaveBalances(year)));
    }

    @GetMapping("/me/payslips")
    @PreAuthorize("hasAuthority('hr:self')")
    public ResponseEntity<ApiResponse<List<PayslipResponse>>> myPayslips() {
        return ResponseEntity.ok(ApiResponse.success(hrService.myPayslips()));
    }
}
