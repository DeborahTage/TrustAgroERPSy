package com.trustagro.hr.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.finance.entity.BusinessUnit;
import com.trustagro.finance.entity.PaymentMethod;
import com.trustagro.finance.entity.TransactionType;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.hr.dto.*;
import com.trustagro.hr.entity.*;
import com.trustagro.hr.repository.*;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.security.Permissions;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Backend-authority tests for {@link HrService}: employee records and numbering,
 * attendance, leave balances, payroll arithmetic and the Finance postings payroll
 * makes. Accounts are asserted by code so a chart-of-accounts rename cannot
 * silently redirect a payroll posting.
 */
@ExtendWith(MockitoExtension.class)
class HrServiceTest {

    private static final String SALARY_EXPENSE = "5210";
    private static final String SALARY_PAYABLE = "2300";
    private static final String PAYROLL_DEDUCTIONS = "2310";
    private static final String CASH = "1000";
    private static final String BANK = "1010";

    @Mock HrDepartmentRepository departmentRepository;
    @Mock HrPositionRepository positionRepository;
    @Mock EmployeeProfileRepository employeeRepository;
    @Mock EmployeeSalaryHistoryRepository salaryHistoryRepository;
    @Mock AttendanceRecordRepository attendanceRepository;
    @Mock LeaveTypeRepository leaveTypeRepository;
    @Mock LeaveRequestRepository leaveRequestRepository;
    @Mock LeaveBalanceRepository leaveBalanceRepository;
    @Mock PayrollDeductionTypeRepository deductionTypeRepository;
    @Mock PayrollPeriodRepository payrollPeriodRepository;
    @Mock PayrollRunRepository payrollRunRepository;
    @Mock PayrollItemRepository payrollItemRepository;
    @Mock PayslipRepository payslipRepository;
    @Mock PayrollPaymentRepository payrollPaymentRepository;
    @Mock HrNumberSequenceRepository sequenceRepository;
    @Mock UserRepository userRepository;
    @Mock FinanceService financeService;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;

    @InjectMocks HrService service;

    private final AtomicLong ids = new AtomicLong(1);
    private final Map<String, HrNumberSequence> sequences = new HashMap<>();

    private User hrUser;
    private User staffUser;
    private User fieldUser;
    private User unlinkedUser;
    private HrDepartment department;
    private HrPosition position;
    private EmployeeProfile employee;
    private EmployeeProfile otherEmployee;
    private LeaveType annualLeave;

    @BeforeEach
    void setUp() {
        setAuth(Permissions.HR_READ, Permissions.HR_CREATE, Permissions.HR_UPDATE,
                Permissions.HR_TERMINATE, Permissions.HR_ATTENDANCE, Permissions.HR_LEAVE,
                Permissions.HR_LEAVE_APPROVE, Permissions.HR_PAYROLL, Permissions.HR_PAYROLL_APPROVE,
                Permissions.HR_PAYROLL_PROCESS, Permissions.HR_REPORT, Permissions.HR_SELF);

        hrUser = user(1L, "hr@test.com", "HR Officer");
        staffUser = user(2L, "staff@test.com", "Staff Member");
        fieldUser = user(3L, "field@test.com", "Field Officer");
        unlinkedUser = user(4L, "unlinked@test.com", "New Hire");
        for (User u : List.of(hrUser, staffUser, fieldUser, unlinkedUser)) {
            lenient().when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
            lenient().when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }

        department = new HrDepartment();
        setId(department, 10L);
        department.setCode("OPS");
        department.setName("Operations");
        lenient().when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));

        position = new HrPosition();
        setId(position, 11L);
        position.setCode("FIELD_OFFICER");
        position.setName("Field Officer");
        lenient().when(positionRepository.findById(11L)).thenReturn(Optional.of(position));

        employee = employeeProfile(100L, "EMP-000001", "Abebe", "Bekele", "20000.00", staffUser);
        otherEmployee = employeeProfile(101L, "EMP-000002", "Kebede", "Alemu", "10000.00", fieldUser);
        lenient().when(employeeRepository.findById(100L)).thenReturn(Optional.of(employee));
        lenient().when(employeeRepository.findById(101L)).thenReturn(Optional.of(otherEmployee));
        lenient().when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());
        lenient().when(employeeRepository.findByUserId(2L)).thenReturn(Optional.of(employee));
        lenient().when(employeeRepository.findByUserId(3L)).thenReturn(Optional.of(otherEmployee));
        lenient().when(employeeRepository.findByUserId(4L)).thenReturn(Optional.empty());

        annualLeave = new LeaveType();
        setId(annualLeave, 20L);
        annualLeave.setCode("ANNUAL");
        annualLeave.setName("Annual Leave");
        annualLeave.setBalanceControlled(true);
        annualLeave.setDefaultAnnualEntitlement(new BigDecimal("20.00"));
        lenient().when(leaveTypeRepository.findById(20L)).thenReturn(Optional.of(annualLeave));

        lenient().when(sequenceRepository.findByNameForUpdate(anyString())).thenAnswer(inv ->
                Optional.of(sequences.computeIfAbsent(inv.getArgument(0), name -> {
                    HrNumberSequence seq = new HrNumberSequence();
                    seq.setName(name);
                    seq.setNextValue(1L);
                    return seq;
                })));
        lenient().when(sequenceRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        lenient().when(employeeRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(salaryHistoryRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(attendanceRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(leaveRequestRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(leaveBalanceRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(payrollPeriodRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(payrollItemRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(payslipRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(payrollPaymentRepository.saveAndFlush(any())).thenAnswer(inv -> withId(inv.getArgument(0)));

        lenient().when(payrollPeriodRepository.findOverlapping(any(), any())).thenReturn(List.of());
        lenient().when(payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(any())).thenReturn(List.of());
        lenient().when(payrollItemRepository.findByPayrollRunIdOrderByIdAsc(any())).thenReturn(List.of());
        lenient().when(payslipRepository.findByPayrollItemId(any())).thenReturn(Optional.empty());
        lenient().when(deductionTypeRepository.findByActiveTrueOrderByCodeAsc()).thenReturn(List.of());
        lenient().when(salaryHistoryRepository.findEffectiveOn(any(), any())).thenReturn(List.of());
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ─── Employees: numbering, linkage, audit ────────────────────

    @Test
    void employeeCreationGeneratesSequentialEmployeeNumbers() {
        EmployeeResponse first = service.createEmployee(employeeRequest("Abebe", "Bekele", "20000.00"));
        assertEquals("EMP-000001", first.getEmployeeNumber());
        assertEquals(EmploymentStatus.ACTIVE, first.getEmploymentStatus());
        assertEquals("Abebe Bekele", first.getFullName());

        EmployeeResponse second = service.createEmployee(employeeRequest("Kebede", "Alemu", "15000.00"));
        assertEquals("EMP-000002", second.getEmployeeNumber());

        assertEquals(3L, sequences.get("EMPLOYEE").getNextValue());
        verify(sequenceRepository, times(2)).save(any());
        verify(auditService, times(2)).log(eq("EMPLOYEE_CREATED"), eq("HR"),
                eq("EmployeeProfile"), any(), isNull(), any());
        verify(notificationService, times(2)).createRoleNotification(
                eq("New Employee Record"), any(), eq(Role.HR));
    }

    @Test
    void employeeNumberSequenceIsSeededWhenMissing() {
        when(sequenceRepository.findByNameForUpdate("EMPLOYEE")).thenReturn(Optional.empty());

        assertEquals("EMP-000001",
                service.createEmployee(employeeRequest("Abebe", "Bekele", "20000.00")).getEmployeeNumber());
        verify(sequenceRepository).saveAndFlush(any());
    }

    @Test
    void employeeCreationRecordsOpeningSalaryHistory() {
        service.createEmployee(employeeRequest("Abebe", "Bekele", "20000.00"));

        ArgumentCaptor<EmployeeSalaryHistory> captor = ArgumentCaptor.forClass(EmployeeSalaryHistory.class);
        verify(salaryHistoryRepository).save(captor.capture());
        EmployeeSalaryHistory opening = captor.getValue();
        assertEquals(0, new BigDecimal("20000.00").compareTo(opening.getBasicSalary()));
        assertEquals(LocalDate.of(2026, 1, 15), opening.getEffectiveDate());
        assertEquals(hrUser, opening.getApprovedBy());
    }

    @Test
    void employeeWithZeroSalaryRecordsNoSalaryHistory() {
        service.createEmployee(employeeRequest("Abebe", "Bekele", "0"));
        verify(salaryHistoryRepository, never()).save(any());
    }

    @Test
    void negativeSalaryRejectedOnCreate() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createEmployee(employeeRequest("Abebe", "Bekele", "-1")));
        assertTrue(ex.getMessage().contains("negative"));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void linkingUserEnablesErpAccess() {
        EmployeeRequest req = employeeRequest("New", "Hire", "12000.00");
        req.setUserId(4L);

        EmployeeResponse created = service.createEmployee(req);
        assertTrue(created.isErpAccessEnabled());
        assertEquals(4L, created.getUserId());

        EmployeeResponse withoutLogin = service.createEmployee(employeeRequest("No", "Login", "9000.00"));
        assertFalse(withoutLogin.isErpAccessEnabled());
        assertNull(withoutLogin.getUserId());
    }

    @Test
    void userAlreadyLinkedToAnotherEmployeeIsRejected() {
        EmployeeRequest req = employeeRequest("Duplicate", "Link", "12000.00");
        req.setUserId(2L); // staffUser already owns EMP-000001

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createEmployee(req));
        assertTrue(ex.getMessage().contains("already linked"));
        assertTrue(ex.getMessage().contains("EMP-000001"));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void unknownUserCannotBeLinked() {
        EmployeeRequest req = employeeRequest("Ghost", "User", "12000.00");
        req.setUserId(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createEmployee(req));
    }

    @Test
    void relinkingSameUserToOwnRecordIsAllowed() {
        EmployeeRequest req = employeeRequest("Abebe", "Bekele", "20000.00");
        req.setUserId(2L);

        EmployeeResponse updated = service.updateEmployee(100L, req);
        assertEquals(2L, updated.getUserId());
        verify(auditService).log(eq("EMPLOYEE_UPDATED"), eq("HR"), eq("EmployeeProfile"),
                eq(100L), isNull(), eq("EMP-000001"));
    }

    @Test
    void updateEmployeeNeverMovesSalaryOutsideSalaryHistory() {
        EmployeeRequest req = employeeRequest("Abebe", "Bekele", "99999.00");

        service.updateEmployee(100L, req);
        assertEquals(0, new BigDecimal("20000.00").compareTo(employee.getBasicSalary()));
        verify(salaryHistoryRepository, never()).save(any());
    }

    // ─── Employees: termination ──────────────────────────────────

    @Test
    void terminationIsASoftStatusAndRecordStaysReadable() {
        EmployeeStatusRequest req = new EmployeeStatusRequest();
        req.setStatus(EmploymentStatus.TERMINATED);
        req.setEffectiveDate(LocalDate.of(2026, 6, 30));
        req.setReason("Contract ended");

        EmployeeResponse terminated = service.changeEmployeeStatus(100L, req);
        assertEquals(EmploymentStatus.TERMINATED, terminated.getEmploymentStatus());
        assertEquals(LocalDate.of(2026, 6, 30), terminated.getTerminationDate());
        assertEquals("Contract ended", terminated.getTerminationReason());

        verify(auditService).log(eq("EMPLOYEE_TERMINATED"), eq("HR"), eq("EmployeeProfile"),
                eq(100L), eq("ACTIVE"), eq("TERMINATED"));
        verify(notificationService).createRoleNotification(
                eq("Employee Status Changed"), any(), eq(Role.HR));

        // The row is retained, not deleted — it is still readable afterwards.
        verify(employeeRepository, never()).delete(any());
        assertEquals("EMP-000001", service.getEmployee(100L).getEmployeeNumber());
        assertEquals(EmploymentStatus.TERMINATED, service.getEmployee(100L).getEmploymentStatus());
    }

    @Test
    void terminationRequiresTerminateAuthority() {
        setAuth(Permissions.HR_READ, Permissions.HR_UPDATE);
        EmployeeStatusRequest req = new EmployeeStatusRequest();
        req.setStatus(EmploymentStatus.TERMINATED);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeEmployeeStatus(100L, req));
        assertTrue(ex.getMessage().contains(Permissions.HR_TERMINATE));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void nonExitingStatusChangeNeedsNoTerminateAuthority() {
        setAuth(Permissions.HR_READ, Permissions.HR_UPDATE);
        EmployeeStatusRequest req = new EmployeeStatusRequest();
        req.setStatus(EmploymentStatus.SUSPENDED);

        assertEquals(EmploymentStatus.SUSPENDED, service.changeEmployeeStatus(100L, req).getEmploymentStatus());
        assertNull(employee.getTerminationDate());
        verify(auditService).log(eq("EMPLOYEE_STATUS_CHANGED"), eq("HR"), eq("EmployeeProfile"),
                eq(100L), eq("ACTIVE"), eq("SUSPENDED"));
    }

    @Test
    void terminatedEmployeesAreExcludedFromPayrollEligibility() {
        assertFalse(EmploymentStatus.TERMINATED.isPayrollEligible());
        assertFalse(EmploymentStatus.RESIGNED.isPayrollEligible());
        assertTrue(EmploymentStatus.ACTIVE.isPayrollEligible());
        assertTrue(EmploymentStatus.ON_LEAVE.isPayrollEligible());

        employee.setEmploymentStatus(EmploymentStatus.TERMINATED);
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.OPEN);
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.save(any())).thenAnswer(inv -> withRunId(inv.getArgument(0)));
        when(employeeRepository.findPayrollEligible(any())).thenReturn(List.of());

        PayrollRunResponse run = service.calculatePayroll(300L);
        assertEquals(0, run.getEmployeeCount());
        assertEquals(0, BigDecimal.ZERO.compareTo(run.getGrossAmount()));

        ArgumentCaptor<Collection<EmploymentStatus>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(employeeRepository).findPayrollEligible(captor.capture());
        Collection<EmploymentStatus> requested = captor.getValue();
        assertTrue(requested.containsAll(List.of(EmploymentStatus.ACTIVE, EmploymentStatus.ON_LEAVE)));
        assertFalse(requested.contains(EmploymentStatus.TERMINATED));
        assertFalse(requested.contains(EmploymentStatus.RESIGNED));
        verify(payrollItemRepository, never()).save(any());
    }

    // ─── Employees: compensation visibility ──────────────────────

    @Test
    void selfServiceOnlyCallerCannotSeeOthersCompensation() {
        setAuthAs("field@test.com", Permissions.HR_SELF);
        assertFalse(service.canSeeCompensation());

        EmployeeResponse other = service.getEmployee(100L);
        assertEquals("EMP-000001", other.getEmployeeNumber());
        assertNull(other.getBasicSalary());
        assertNull(other.getBankName());
        assertNull(other.getBankAccountRef());

        when(employeeRepository.findAllOrdered()).thenReturn(List.of(employee, otherEmployee));
        assertTrue(service.listEmployees(null, null, null).stream()
                .allMatch(e -> e.getBasicSalary() == null));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.listSalaryHistory(100L));
        assertTrue(ex.getMessage().contains(Permissions.HR_PAYROLL));
    }

    @Test
    void payrollAndHrOperatorsSeeCompensation() {
        assertTrue(service.canSeeCompensation());
        assertEquals(0, new BigDecimal("20000.00").compareTo(service.getEmployee(100L).getBasicSalary()));

        setAuth(Permissions.HR_READ, Permissions.FINANCE_READ);
        assertTrue(service.canSeeCompensation());

        setAuth(Permissions.FINANCE_READ);
        assertFalse(service.canSeeCompensation());
    }

    // ─── Attendance ──────────────────────────────────────────────

    @Test
    void attendanceCreationDerivesHoursWorked() {
        AttendanceRequest req = attendanceRequest(LocalDate.of(2026, 3, 2),
                LocalTime.of(8, 0), LocalTime.of(17, 30));

        AttendanceResponse saved = service.recordAttendance(req);
        assertEquals(0, new BigDecimal("9.50").compareTo(saved.getHoursWorked()));
        assertEquals(AttendanceStatus.PRESENT, saved.getStatus());
        assertEquals(100L, saved.getEmployeeId());
        assertEquals("HR Officer", saved.getRecordedByName());
        verify(auditService).log(eq("ATTENDANCE_CREATED"), eq("HR"), eq("AttendanceRecord"),
                any(), isNull(), eq("EMP-000001@2026-03-02"));
    }

    @Test
    void duplicateAttendanceForEmployeeAndDateIsRejected() {
        LocalDate day = LocalDate.of(2026, 3, 2);
        when(attendanceRepository.existsByEmployeeIdAndAttendanceDate(100L, day)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.recordAttendance(
                attendanceRequest(day, LocalTime.of(8, 0), LocalTime.of(17, 0))));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void attendanceDurationHandlesOvernightShifts() {
        assertEquals(0, new BigDecimal("9.00").compareTo(
                service.calculateHoursWorked(LocalTime.of(8, 0), LocalTime.of(17, 0))));
        assertEquals(0, new BigDecimal("8.00").compareTo(
                service.calculateHoursWorked(LocalTime.of(22, 0), LocalTime.of(6, 0))));
        assertEquals(0, new BigDecimal("7.75").compareTo(
                service.calculateHoursWorked(LocalTime.of(23, 15), LocalTime.of(7, 0))));
        assertEquals(0, new BigDecimal("0.50").compareTo(
                service.calculateHoursWorked(LocalTime.of(23, 45), LocalTime.of(0, 15))));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                service.calculateHoursWorked(null, LocalTime.of(17, 0))));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                service.calculateHoursWorked(LocalTime.of(8, 0), null)));
    }

    @Test
    void overnightAttendanceRecordStoresRolledOverHours() {
        AttendanceResponse saved = service.recordAttendance(attendanceRequest(
                LocalDate.of(2026, 3, 2), LocalTime.of(21, 0), LocalTime.of(5, 30)));
        assertEquals(0, new BigDecimal("8.50").compareTo(saved.getHoursWorked()));
    }

    @Test
    void attendanceUpdateRecalculatesHoursAndAudits() {
        AttendanceRecord record = new AttendanceRecord();
        setId(record, 150L);
        record.setEmployee(employee);
        record.setAttendanceDate(LocalDate.of(2026, 3, 2));
        record.setStatus(AttendanceStatus.PRESENT);
        record.setHoursWorked(new BigDecimal("9.50"));
        when(attendanceRepository.findById(150L)).thenReturn(Optional.of(record));

        AttendanceRequest req = attendanceRequest(LocalDate.of(2026, 3, 2),
                LocalTime.of(8, 0), LocalTime.of(12, 0));
        req.setStatus(AttendanceStatus.HALF_DAY);

        AttendanceResponse updated = service.updateAttendance(150L, req);
        assertEquals(0, new BigDecimal("4.00").compareTo(updated.getHoursWorked()));
        assertEquals(AttendanceStatus.HALF_DAY, updated.getStatus());
        verify(auditService).log(eq("ATTENDANCE_UPDATED"), eq("HR"), eq("AttendanceRecord"),
                eq(150L), eq("PRESENT/9.50"), eq("HALF_DAY/4.00"));
    }

    // ─── Leave requests ─────────────────────────────────────────

    @Test
    void leaveRequestUsesBackendComputedTotalDays() {
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(100L, 20L, 2026))
                .thenReturn(Optional.of(leaveBalance(employee, "20.00", "0.00")));

        LeaveRequestResponse created = service.createLeaveRequest(
                leaveRequest(100L, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6)), false);

        assertEquals("LV-000001", created.getRequestNumber());
        assertEquals(LeaveRequestStatus.PENDING, created.getStatus());
        assertEquals(0, new BigDecimal("5.00").compareTo(created.getTotalDays()));
        assertEquals(100L, created.getEmployeeId());
        verify(auditService).log(eq("LEAVE_REQUESTED"), eq("HR"), eq("LeaveRequest"),
                any(), isNull(), eq("LV-000001"));
        verify(notificationService).createRoleNotification(
                eq("Leave Request Submitted"), any(), eq(Role.HR));
    }

    @Test
    void singleDayLeaveCountsAsOneDay() {
        assertEquals(0, new BigDecimal("1.00").compareTo(
                service.calculateLeaveDays(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2))));
        assertEquals(0, new BigDecimal("31.00").compareTo(
                service.calculateLeaveDays(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))));
    }

    @Test
    void leaveRequestRejectsInvertedDatesInactiveTypeAndMissingEmployee() {
        BusinessException inverted = assertThrows(BusinessException.class, () -> service.createLeaveRequest(
                leaveRequest(100L, LocalDate.of(2026, 3, 6), LocalDate.of(2026, 3, 2)), false));
        assertTrue(inverted.getMessage().contains("cannot precede"));

        annualLeave.setActive(false);
        BusinessException inactive = assertThrows(BusinessException.class, () -> service.createLeaveRequest(
                leaveRequest(100L, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6)), false));
        assertTrue(inactive.getMessage().contains("not active"));

        BusinessException missing = assertThrows(BusinessException.class, () -> service.createLeaveRequest(
                leaveRequest(null, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6)), false));
        assertTrue(missing.getMessage().contains("employeeId is required"));

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void leaveRequestRejectedWhenBalanceIsShort() {
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(100L, 20L, 2026))
                .thenReturn(Optional.of(leaveBalance(employee, "20.00", "18.00")));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createLeaveRequest(
                leaveRequest(100L, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 6)), false));
        assertTrue(ex.getMessage().contains("Insufficient ANNUAL balance"));
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void selfServiceLeaveIgnoresClientSuppliedEmployeeId() {
        setAuthAs("staff@test.com", Permissions.HR_SELF);
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(100L, 20L, 2026))
                .thenReturn(Optional.of(leaveBalance(employee, "20.00", "0.00")));

        LeaveRequestCreateRequest req = leaveRequest(101L, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 3));
        LeaveRequestResponse created = service.createLeaveRequest(req, true);

        assertEquals(100L, created.getEmployeeId());
        assertEquals("EMP-000001", created.getEmployeeNumber());
    }

    @Test
    void leaveApprovalDrawsDownTheBalance() {
        LeaveRequest request = pendingLeave(200L, otherEmployee, "5.00");
        LeaveBalance balance = leaveBalance(otherEmployee, "20.00", "0.00");
        when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(request));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(101L, 20L, 2026))
                .thenReturn(Optional.of(balance));

        LeaveRequestResponse approved = service.approveLeaveRequest(200L);
        assertEquals(LeaveRequestStatus.APPROVED, approved.getStatus());
        assertEquals("HR Officer", approved.getApprovedByName());
        assertNotNull(approved.getApprovedAt());

        assertEquals(0, new BigDecimal("5.00").compareTo(balance.getUsedDays()));
        assertEquals(0, new BigDecimal("15.00").compareTo(balance.getRemainingDays()));
        verify(leaveBalanceRepository).save(balance);
        verify(auditService).log(eq("LEAVE_APPROVED"), eq("HR"), eq("LeaveRequest"),
                eq(200L), eq("PENDING"), eq("APPROVED"));
        verify(notificationService).createUserNotification(eq("Leave Approved"), any(), eq(fieldUser));
    }

    @Test
    void leaveApprovalFailsWhenBalanceIsInsufficient() {
        LeaveRequest request = pendingLeave(200L, otherEmployee, "5.00");
        when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(request));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(101L, 20L, 2026))
                .thenReturn(Optional.of(leaveBalance(otherEmployee, "20.00", "17.00")));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approveLeaveRequest(200L));
        assertTrue(ex.getMessage().contains("Insufficient ANNUAL balance"));
        assertEquals(LeaveRequestStatus.PENDING, request.getStatus());
        verify(leaveRequestRepository, never()).save(any());
        verify(leaveBalanceRepository, never()).save(any());
    }

    @Test
    void uncontrolledLeaveTypeSkipsBalanceEnforcement() {
        annualLeave.setBalanceControlled(false);
        LeaveRequest request = pendingLeave(200L, otherEmployee, "40.00");
        when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(request));

        assertEquals(LeaveRequestStatus.APPROVED, service.approveLeaveRequest(200L).getStatus());
        verify(leaveBalanceRepository, never()).save(any());
    }

    @Test
    void approvedLeaveCannotBeApprovedOrRejectedAgain() {
        LeaveRequest request = pendingLeave(200L, otherEmployee, "5.00");
        request.setStatus(LeaveRequestStatus.APPROVED);
        when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(request));

        assertThrows(BusinessException.class, () -> service.approveLeaveRequest(200L));
        LeaveDecisionRequest decision = new LeaveDecisionRequest();
        decision.setReason("Too late");
        assertThrows(BusinessException.class, () -> service.rejectLeaveRequest(200L, decision));
    }

    @Test
    void leaveRejectionStoresReasonAndNotifiesEmployee() {
        LeaveRequest request = pendingLeave(200L, otherEmployee, "5.00");
        when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(request));

        LeaveDecisionRequest decision = new LeaveDecisionRequest();
        decision.setReason("  Peak season  ");

        LeaveRequestResponse rejected = service.rejectLeaveRequest(200L, decision);
        assertEquals(LeaveRequestStatus.REJECTED, rejected.getStatus());
        assertEquals("Peak season", rejected.getRejectionReason());
        verify(auditService).log(eq("LEAVE_REJECTED"), eq("HR"), eq("LeaveRequest"),
                eq(200L), eq("PENDING"), eq("REJECTED"));
        verify(notificationService).createUserNotification(eq("Leave Rejected"), any(), eq(fieldUser));
        verify(leaveBalanceRepository, never()).save(any());
    }

    @Test
    void leaveRejectionRequiresAReason() {
        LeaveRequest request = pendingLeave(200L, otherEmployee, "5.00");
        when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(request));

        assertThrows(BusinessException.class, () -> service.rejectLeaveRequest(200L, null));
        LeaveDecisionRequest blank = new LeaveDecisionRequest();
        blank.setReason("   ");
        assertThrows(BusinessException.class, () -> service.rejectLeaveRequest(200L, blank));
        assertEquals(LeaveRequestStatus.PENDING, request.getStatus());
    }

    @Test
    void leaveDecisionsRequireApprovalAuthority() {
        setAuth(Permissions.HR_READ, Permissions.HR_LEAVE);
        LeaveDecisionRequest decision = new LeaveDecisionRequest();
        decision.setReason("No");

        BusinessException approve = assertThrows(BusinessException.class,
                () -> service.approveLeaveRequest(200L));
        assertTrue(approve.getMessage().contains(Permissions.HR_LEAVE_APPROVE));
        assertThrows(BusinessException.class, () -> service.rejectLeaveRequest(200L, decision));
        verifyNoInteractions(leaveRequestRepository);
    }

    @Test
    void approverCannotApproveTheirOwnLeaveRequest() {
        setAuthAs("staff@test.com", Permissions.HR_LEAVE_APPROVE);
        LeaveRequest request = pendingLeave(200L, employee, "5.00");
        when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(request));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approveLeaveRequest(200L));
        assertTrue(ex.getMessage().contains("cannot approve your own"));
        assertEquals(LeaveRequestStatus.PENDING, request.getStatus());
        verify(leaveBalanceRepository, never()).save(any());
    }

    @Test
    void unrelatedEmployeeCannotCancelSomeoneElsesLeave() {
        setAuthAs("field@test.com", Permissions.HR_SELF);
        LeaveRequest request = pendingLeave(200L, employee, "5.00");
        when(leaveRequestRepository.findById(200L)).thenReturn(Optional.of(request));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.cancelLeaveRequest(200L));
        assertTrue(ex.getMessage().contains("Not authorized"));

        setAuthAs("staff@test.com", Permissions.HR_SELF);
        assertEquals(LeaveRequestStatus.CANCELLED, service.cancelLeaveRequest(200L).getStatus());
        verify(auditService).log(eq("LEAVE_CANCELLED"), eq("HR"), eq("LeaveRequest"),
                eq(200L), eq("PENDING"), eq("CANCELLED"));
    }

    @Test
    void leaveBalancesAreOnlyReportedForControlledTypes() {
        LeaveType unpaid = new LeaveType();
        setId(unpaid, 21L);
        unpaid.setCode("UNPAID");
        unpaid.setName("Unpaid Leave");
        unpaid.setBalanceControlled(false);
        when(leaveTypeRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(annualLeave, unpaid));
        when(leaveBalanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(100L, 20L, 2026))
                .thenReturn(Optional.empty());

        List<LeaveBalanceResponse> balances = service.getLeaveBalances(100L, 2026);
        assertEquals(1, balances.size());
        assertEquals("ANNUAL", balances.get(0).getLeaveTypeCode());
        assertEquals(0, new BigDecimal("20.00").compareTo(balances.get(0).getEntitlement()));
        assertEquals(0, new BigDecimal("20.00").compareTo(balances.get(0).getRemainingDays()));
        verify(leaveBalanceRepository).save(any());
    }

    // ─── Payroll periods ─────────────────────────────────────────

    @Test
    void payrollPeriodCreationGeneratesCodeAndOpensPeriod() {
        PayrollPeriodRequest req = new PayrollPeriodRequest();
        req.setStartDate(LocalDate.of(2026, 3, 1));
        req.setEndDate(LocalDate.of(2026, 3, 31));

        PayrollPeriodResponse created = service.createPayrollPeriod(req);
        assertEquals("PR-000001", created.getPeriodCode());
        assertEquals(PayrollPeriodStatus.OPEN, created.getStatus());
        assertEquals(0, created.getEmployeeCount());
        assertNull(created.getCurrentRunId());
        assertEquals("HR Officer", created.getCreatedByName());
        verify(auditService).log(eq("PAYROLL_PERIOD_CREATED"), eq("HR"), eq("PayrollPeriod"),
                any(), isNull(), eq("PR-000001"));
    }

    @Test
    void payrollPeriodOverlapIsRejected() {
        PayrollPeriod existing = payrollPeriod(PayrollPeriodStatus.CALCULATED);
        when(payrollPeriodRepository.findOverlapping(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 4, 15)))
                .thenReturn(List.of(existing));

        PayrollPeriodRequest req = new PayrollPeriodRequest();
        req.setStartDate(LocalDate.of(2026, 3, 15));
        req.setEndDate(LocalDate.of(2026, 4, 15));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createPayrollPeriod(req));
        assertTrue(ex.getMessage().contains("overlaps existing period PR-2026-03"));
        verify(payrollPeriodRepository, never()).save(any());
    }

    @Test
    void payrollPeriodRejectsInvertedDatesAndDuplicateCode() {
        PayrollPeriodRequest inverted = new PayrollPeriodRequest();
        inverted.setStartDate(LocalDate.of(2026, 3, 31));
        inverted.setEndDate(LocalDate.of(2026, 3, 1));
        assertThrows(BusinessException.class, () -> service.createPayrollPeriod(inverted));

        PayrollPeriodRequest duplicate = new PayrollPeriodRequest();
        duplicate.setPeriodCode("pr-2026-03");
        duplicate.setStartDate(LocalDate.of(2026, 3, 1));
        duplicate.setEndDate(LocalDate.of(2026, 3, 31));
        when(payrollPeriodRepository.existsByPeriodCode("PR-2026-03")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createPayrollPeriod(duplicate));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(payrollPeriodRepository, never()).save(any());
    }

    // ─── Payroll calculation ─────────────────────────────────────

    @Test
    void payrollCalculationTotalsGrossDeductionsAndNet() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.OPEN);
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.maxRunNumber(300L)).thenReturn(0);
        when(payrollRunRepository.save(any())).thenAnswer(inv -> withRunId(inv.getArgument(0)));
        when(deductionTypeRepository.findByActiveTrueOrderByCodeAsc())
                .thenReturn(List.of(deductionType(30L, "TAX", DeductionCategory.INCOME_TAX, "15")));
        when(employeeRepository.findPayrollEligible(any())).thenReturn(List.of(employee, otherEmployee));

        PayrollRunResponse run = service.calculatePayroll(300L);

        assertEquals(2, run.getEmployeeCount());
        assertEquals(1, run.getRunNumber());
        assertEquals(PayrollRunStatus.CALCULATED, run.getStatus());
        assertEquals(0, new BigDecimal("30000.00").compareTo(run.getGrossAmount()));
        assertEquals(0, new BigDecimal("4500.00").compareTo(run.getDeductionAmount()));
        assertEquals(0, new BigDecimal("25500.00").compareTo(run.getNetAmount()));

        ArgumentCaptor<PayrollItem> items = ArgumentCaptor.forClass(PayrollItem.class);
        verify(payrollItemRepository, times(2)).save(items.capture());
        PayrollItem first = items.getAllValues().get(0);
        assertEquals(0, new BigDecimal("20000.00").compareTo(first.getBasicSalary()));
        assertEquals(0, new BigDecimal("20000.00").compareTo(first.getGrossSalary()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(first.getTax()));
        assertEquals(0, BigDecimal.ZERO.compareTo(first.getPension()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(first.getTotalDeductions()));
        assertEquals(0, new BigDecimal("17000.00").compareTo(first.getNetSalary()));
        assertEquals(PayrollItemStatus.CALCULATED, first.getStatus());
        assertEquals(PayrollPaymentStatus.PENDING, first.getPaymentStatus());

        PayrollItem second = items.getAllValues().get(1);
        assertEquals(0, new BigDecimal("1500.00").compareTo(second.getTotalDeductions()));
        assertEquals(0, new BigDecimal("8500.00").compareTo(second.getNetSalary()));

        ArgumentCaptor<Payslip> payslips = ArgumentCaptor.forClass(Payslip.class);
        verify(payslipRepository, times(2)).save(payslips.capture());
        assertEquals("PS-000001", payslips.getAllValues().get(0).getPayslipNumber());
        assertEquals("PS-000002", payslips.getAllValues().get(1).getPayslipNumber());
        assertEquals(0, new BigDecimal("17000.00").compareTo(payslips.getAllValues().get(0).getNetSalary()));

        assertEquals(PayrollPeriodStatus.CALCULATED, period.getStatus());
        assertNotNull(period.getCalculatedAt());
        verify(auditService).log(eq("PAYROLL_CALCULATED"), eq("HR"), eq("PayrollRun"),
                eq(400L), isNull(), contains("gross 30000.00"));
    }

    @Test
    void payrollMixesPercentageAndFixedDeductionsByCategory() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.OPEN);
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.save(any())).thenAnswer(inv -> withRunId(inv.getArgument(0)));
        when(deductionTypeRepository.findByActiveTrueOrderByCodeAsc()).thenReturn(List.of(
                deductionType(30L, "TAX", DeductionCategory.INCOME_TAX, "15"),
                deductionType(31L, "PENSION", DeductionCategory.PENSION, "7"),
                fixedDeductionType(32L, "UNION", DeductionCategory.OTHER, "50.00")));
        when(employeeRepository.findPayrollEligible(any())).thenReturn(List.of(employee));

        service.calculatePayroll(300L);

        ArgumentCaptor<PayrollItem> captor = ArgumentCaptor.forClass(PayrollItem.class);
        verify(payrollItemRepository).save(captor.capture());
        PayrollItem item = captor.getValue();
        assertEquals(0, new BigDecimal("3000.00").compareTo(item.getTax()));
        assertEquals(0, new BigDecimal("1400.00").compareTo(item.getPension()));
        assertEquals(0, new BigDecimal("50.00").compareTo(item.getOtherDeductions()));
        assertEquals(0, new BigDecimal("4450.00").compareTo(item.getTotalDeductions()));
        assertEquals(0, new BigDecimal("15550.00").compareTo(item.getNetSalary()));
    }

    @Test
    void deductionsExceedingGrossAreRejected() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.OPEN);
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.save(any())).thenAnswer(inv -> withRunId(inv.getArgument(0)));
        when(deductionTypeRepository.findByActiveTrueOrderByCodeAsc()).thenReturn(List.of(
                deductionType(30L, "TAX", DeductionCategory.INCOME_TAX, "60"),
                deductionType(31L, "PENSION", DeductionCategory.PENSION, "60")));
        when(employeeRepository.findPayrollEligible(any())).thenReturn(List.of(employee));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.calculatePayroll(300L));
        assertTrue(ex.getMessage().contains("exceed gross pay"));
        verify(payrollItemRepository, never()).save(any());
    }

    @Test
    void payrollUsesTheSalaryEffectiveForThePeriodNotTheCurrentOne() {
        employee.setBasicSalary(new BigDecimal("25000.00")); // raised after the period closed
        EmployeeSalaryHistory historic = new EmployeeSalaryHistory();
        setId(historic, 120L);
        historic.setEmployee(employee);
        historic.setEffectiveDate(LocalDate.of(2026, 1, 15));
        historic.setBasicSalary(new BigDecimal("20000.00"));
        when(salaryHistoryRepository.findEffectiveOn(100L, LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of(historic));

        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.OPEN);
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.save(any())).thenAnswer(inv -> withRunId(inv.getArgument(0)));
        when(deductionTypeRepository.findByActiveTrueOrderByCodeAsc())
                .thenReturn(List.of(deductionType(30L, "TAX", DeductionCategory.INCOME_TAX, "15")));
        when(employeeRepository.findPayrollEligible(any())).thenReturn(List.of(employee));

        PayrollRunResponse run = service.calculatePayroll(300L);
        assertEquals(0, new BigDecimal("20000.00").compareTo(run.getGrossAmount()));
        assertEquals(0, new BigDecimal("17000.00").compareTo(run.getNetAmount()));

        ArgumentCaptor<PayrollItem> captor = ArgumentCaptor.forClass(PayrollItem.class);
        verify(payrollItemRepository).save(captor.capture());
        assertEquals(0, new BigDecimal("20000.00").compareTo(captor.getValue().getBasicSalary()));
    }

    @Test
    void laterSalaryChangeLeavesExistingPayrollItemsUntouched() {
        PayrollItem issued = payrollItem(500L, payrollRun(payrollPeriod(PayrollPeriodStatus.PROCESSED),
                PayrollRunStatus.PROCESSED, "20000.00", "3000.00", "17000.00"),
                employee, "20000.00", "3000.00", "17000.00");
        issued.setStatus(PayrollItemStatus.PROCESSED);

        SalaryChangeRequest raise = new SalaryChangeRequest();
        raise.setBasicSalary(new BigDecimal("25000.00"));
        raise.setEffectiveDate(LocalDate.of(2026, 4, 1));
        raise.setReason("Promotion");
        service.changeSalary(100L, raise);

        assertEquals(0, new BigDecimal("20000.00").compareTo(issued.getBasicSalary()));
        assertEquals(0, new BigDecimal("17000.00").compareTo(issued.getNetSalary()));
        assertEquals(PayrollItemStatus.PROCESSED, issued.getStatus());
        verifyNoInteractions(payrollItemRepository);
        verifyNoInteractions(payrollRunRepository);
    }

    @Test
    void approvedRunBlocksRecalculationAndClosedPeriodCannotBeCalculated() {
        PayrollPeriod calculated = payrollPeriod(PayrollPeriodStatus.CALCULATED);
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(calculated));
        when(payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(300L)).thenReturn(
                List.of(payrollRun(calculated, PayrollRunStatus.APPROVED, "30000.00", "4500.00", "25500.00")));

        BusinessException frozen = assertThrows(BusinessException.class, () -> service.calculatePayroll(300L));
        assertTrue(frozen.getMessage().contains("cannot be recalculated"));

        for (PayrollPeriodStatus locked : List.of(PayrollPeriodStatus.APPROVED,
                PayrollPeriodStatus.PROCESSED, PayrollPeriodStatus.CLOSED, PayrollPeriodStatus.CANCELLED)) {
            calculated.setStatus(locked);
            BusinessException ex = assertThrows(BusinessException.class, () -> service.calculatePayroll(300L));
            assertTrue(ex.getMessage().contains("cannot be calculated for a " + locked));
        }
        verify(payrollRunRepository, never()).save(any());
        verify(payrollItemRepository, never()).save(any());
    }

    @Test
    void recalculationDiscardsTheUnapprovedRunAndItsPayslips() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.CALCULATED);
        PayrollRun stale = payrollRun(period, PayrollRunStatus.CALCULATED, "10000.00", "1500.00", "8500.00");
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.maxRunNumber(300L)).thenReturn(1);
        when(payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(300L)).thenReturn(List.of(stale));
        when(payrollRunRepository.save(any())).thenAnswer(inv -> withRunId(inv.getArgument(0), 401L));
        when(employeeRepository.findPayrollEligible(any())).thenReturn(List.of());

        PayrollRunResponse run = service.calculatePayroll(300L);
        assertEquals(2, run.getRunNumber());
        verify(payslipRepository).deleteByRunId(400L);
        verify(payrollItemRepository).deleteByPayrollRunId(400L);
        verify(payrollRunRepository).delete(stale);
    }

    // ─── Payroll approval ────────────────────────────────────────

    @Test
    void payrollApprovalFreezesRunAndItems() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.CALCULATED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.CALCULATED, "20000.00", "3000.00", "17000.00");
        PayrollItem item = payrollItem(500L, run, otherEmployee, "20000.00", "3000.00", "17000.00");
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(300L)).thenReturn(List.of(run));
        when(payrollItemRepository.findByPayrollRunIdOrderByIdAsc(400L)).thenReturn(List.of(item));

        PayrollPeriodResponse approved = service.approvePayroll(300L);
        assertEquals(PayrollPeriodStatus.APPROVED, approved.getStatus());
        assertEquals(400L, approved.getCurrentRunId());
        assertEquals("HR Officer", approved.getApprovedByName());
        assertEquals(PayrollRunStatus.APPROVED, run.getStatus());
        assertEquals(PayrollItemStatus.APPROVED, item.getStatus());
        verify(auditService).log(eq("PAYROLL_APPROVED"), eq("HR"), eq("PayrollPeriod"),
                eq(300L), eq("CALCULATED"), eq("APPROVED"));
        verify(notificationService).createRoleNotification(eq("Payroll Approved"), any(), eq(Role.FINANCE));

        // Approving an already approved period is a no-op.
        assertEquals(PayrollPeriodStatus.APPROVED, service.approvePayroll(300L).getStatus());
        verify(auditService, times(1)).log(eq("PAYROLL_APPROVED"), any(), any(), any(), any(), any());
    }

    @Test
    void payrollApprovalRequiresApproveAuthority() {
        setAuth(Permissions.HR_READ, Permissions.HR_PAYROLL);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.approvePayroll(300L));
        assertTrue(ex.getMessage().contains(Permissions.HR_PAYROLL_APPROVE));
        verifyNoInteractions(payrollPeriodRepository);
    }

    @Test
    void approverCannotApproveARunContainingTheirOwnPay() {
        setAuthAs("staff@test.com", Permissions.HR_PAYROLL_APPROVE);
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.CALCULATED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.CALCULATED, "20000.00", "3000.00", "17000.00");
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(300L)).thenReturn(List.of(run));
        when(payrollItemRepository.findByPayrollRunIdOrderByIdAsc(400L))
                .thenReturn(List.of(payrollItem(500L, run, employee, "20000.00", "3000.00", "17000.00")));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approvePayroll(300L));
        assertTrue(ex.getMessage().contains("your own pay"));
        assertEquals(PayrollRunStatus.CALCULATED, run.getStatus());
        assertEquals(PayrollPeriodStatus.CALCULATED, period.getStatus());
    }

    @Test
    void approvalRequiresACalculatedRun() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.CALCULATED);
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.approvePayroll(300L));
        assertTrue(ex.getMessage().contains("Calculate payroll first"));
    }

    // ─── Payroll processing → Finance ────────────────────────────

    @Test
    void processingPostsGrossExpenseAndReclassesDeductions() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.APPROVED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.APPROVED, "30000.00", "4500.00", "25500.00");
        PayrollItem item = payrollItem(500L, run, employee, "20000.00", "3000.00", "17000.00");
        item.setStatus(PayrollItemStatus.APPROVED);
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(300L)).thenReturn(List.of(run));
        when(payrollItemRepository.findByPayrollRunIdOrderByIdAsc(400L)).thenReturn(List.of(item));

        PayrollPeriodResponse processed = service.processPayroll(300L);
        assertEquals(PayrollPeriodStatus.PROCESSED, processed.getStatus());
        assertEquals(PayrollRunStatus.PROCESSED, run.getStatus());
        assertEquals(PayrollItemStatus.PROCESSED, item.getStatus());

        verify(financeService).postTransaction(eq(TransactionType.EXPENSE),
                eq(new BigDecimal("30000.00")), eq(BusinessUnit.GENERAL),
                eq(SALARY_EXPENSE), eq(SALARY_PAYABLE), contains("gross salary expense"),
                eq("HR"), eq("HR_PAYROLL_EXPENSE"), eq(400L));
        verify(financeService).postTransaction(eq(TransactionType.TRANSFER),
                eq(new BigDecimal("4500.00")), eq(BusinessUnit.GENERAL),
                eq(SALARY_PAYABLE), eq(PAYROLL_DEDUCTIONS), contains("withheld deductions"),
                eq("HR"), eq("HR_PAYROLL_DEDUCTION"), eq(400L));
        verifyNoMoreInteractions(financeService);

        verify(auditService).log(eq("PAYROLL_PROCESSED"), eq("HR"), eq("PayrollPeriod"),
                eq(300L), eq("APPROVED"), contains("net 25500.00"));
        verify(notificationService).createUserNotification(eq("Payslip Available"), any(), eq(staffUser));
        verify(notificationService).createRoleNotification(
                eq("Payroll Posted to Finance"), any(), eq(Role.FINANCE));
    }

    @Test
    void processingIsIdempotentAndPostsEachReferenceTypeOnce() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.APPROVED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.APPROVED, "30000.00", "4500.00", "25500.00");
        PayrollItem item = payrollItem(500L, run, employee, "20000.00", "3000.00", "17000.00");
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(300L)).thenReturn(List.of(run));
        when(payrollItemRepository.findByPayrollRunIdOrderByIdAsc(400L)).thenReturn(List.of(item));

        service.processPayroll(300L);
        PayrollPeriodResponse second = service.processPayroll(300L);

        // Second call short-circuits on the PROCESSED status guard — no second posting, no second audit.
        assertEquals(PayrollPeriodStatus.PROCESSED, second.getStatus());
        verify(financeService, times(1)).postTransaction(eq(TransactionType.EXPENSE), any(), any(),
                any(), any(), any(), any(), eq("HR_PAYROLL_EXPENSE"), any());
        verify(financeService, times(1)).postTransaction(eq(TransactionType.TRANSFER), any(), any(),
                any(), any(), any(), any(), eq("HR_PAYROLL_DEDUCTION"), any());
        verify(auditService, times(1)).log(eq("PAYROLL_PROCESSED"), any(), any(), any(), any(), any());
        verify(payrollRunRepository, times(1)).save(run);
    }

    @Test
    void processingSkipsPostingsWithNothingToPost() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.APPROVED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.APPROVED, "0.00", "0.00", "0.00");
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(300L)).thenReturn(List.of(run));

        assertEquals(PayrollPeriodStatus.PROCESSED, service.processPayroll(300L).getStatus());
        verifyNoInteractions(financeService);
    }

    @Test
    void processingRequiresProcessAuthorityAndAnApprovedRun() {
        setAuth(Permissions.HR_READ, Permissions.HR_PAYROLL, Permissions.HR_PAYROLL_APPROVE);
        BusinessException denied = assertThrows(BusinessException.class, () -> service.processPayroll(300L));
        assertTrue(denied.getMessage().contains(Permissions.HR_PAYROLL_PROCESS));
        verifyNoInteractions(financeService);

        setAuth(Permissions.HR_PAYROLL_PROCESS);
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.APPROVED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.CALCULATED, "30000.00", "4500.00", "25500.00");
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(300L)).thenReturn(List.of(run));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.processPayroll(300L));
        assertTrue(ex.getMessage().contains("must be APPROVED before processing"));
        verifyNoInteractions(financeService);
    }

    @Test
    void unapprovedPeriodCannotBeProcessed() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.CALCULATED);
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.processPayroll(300L));
        assertTrue(ex.getMessage().contains("Invalid payroll period status transition"));
        verifyNoInteractions(financeService);
    }

    /**
     * The worked accounting example: one employee on 20 000, a single configured
     * 15 % income-tax deduction (3 000), leaving 17 000 net. Gross hits the expense
     * account, the withholding is reclassed, and the disbursement settles the payable.
     */
    @Test
    void accountingScenarioTwentyThousandGrossThreeThousandDeductionsSeventeenThousandNet() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.OPEN);
        List<PayrollRun> runs = new ArrayList<>();
        List<PayrollItem> items = new ArrayList<>();
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.save(any())).thenAnswer(inv -> {
            PayrollRun saved = withRunId(inv.getArgument(0));
            if (!runs.contains(saved)) runs.add(saved);
            return saved;
        });
        when(payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(300L))
                .thenAnswer(inv -> List.copyOf(runs));
        when(payrollItemRepository.save(any())).thenAnswer(inv -> {
            PayrollItem saved = withId(inv.getArgument(0));
            if (!items.contains(saved)) items.add(saved);
            return saved;
        });
        when(payrollItemRepository.findByPayrollRunIdOrderByIdAsc(400L)).thenAnswer(inv -> List.copyOf(items));
        when(deductionTypeRepository.findByActiveTrueOrderByCodeAsc())
                .thenReturn(List.of(deductionType(30L, "TAX", DeductionCategory.INCOME_TAX, "15")));
        when(employeeRepository.findPayrollEligible(any())).thenReturn(List.of(employee));
        when(payrollRunRepository.findById(400L)).thenAnswer(inv -> Optional.of(runs.get(0)));

        PayrollRunResponse calculated = service.calculatePayroll(300L);
        assertEquals(0, new BigDecimal("20000.00").compareTo(calculated.getGrossAmount()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(calculated.getDeductionAmount()));
        assertEquals(0, new BigDecimal("17000.00").compareTo(calculated.getNetAmount()));

        service.approvePayroll(300L);
        service.processPayroll(300L);

        verify(financeService).postTransaction(eq(TransactionType.EXPENSE),
                eq(new BigDecimal("20000.00")), eq(BusinessUnit.GENERAL),
                eq(SALARY_EXPENSE), eq(SALARY_PAYABLE), any(),
                eq("HR"), eq("HR_PAYROLL_EXPENSE"), eq(400L));
        verify(financeService).postTransaction(eq(TransactionType.TRANSFER),
                eq(new BigDecimal("3000.00")), eq(BusinessUnit.GENERAL),
                eq(SALARY_PAYABLE), eq(PAYROLL_DEDUCTIONS), any(),
                eq("HR"), eq("HR_PAYROLL_DEDUCTION"), eq(400L));

        PayrollPaymentRequest pay = new PayrollPaymentRequest();
        pay.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        PayrollPaymentResponse payment = service.payPayroll(400L, pay);
        assertEquals(0, new BigDecimal("17000.00").compareTo(payment.getAmount()));

        verify(financeService).postTransaction(eq(TransactionType.TRANSFER),
                eq(new BigDecimal("17000.00")), eq(BusinessUnit.GENERAL),
                eq(SALARY_PAYABLE), eq(BANK), any(), eq("HR"), eq("HR_PAYROLL_PAYMENT"),
                any(), eq(PaymentMethod.BANK_TRANSFER), any());

        // Salary expense is recognised exactly once, at processing — never again at payment.
        verify(financeService, times(1)).postTransaction(any(), any(), any(),
                eq(SALARY_EXPENSE), any(), any(), any(), any(), any());
        verify(financeService, never()).postTransaction(any(), any(), any(),
                eq(SALARY_EXPENSE), any(), any(), any(), any(), any(), any(), any());
    }

    // ─── Payroll payments ────────────────────────────────────────

    @Test
    void payrollPaymentSettlesPayableAndNeverTouchesSalaryExpense() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.PROCESSED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.PROCESSED, "30000.00", "4500.00", "25500.00");
        PayrollItem first = payrollItem(500L, run, employee, "20000.00", "3000.00", "17000.00");
        PayrollItem second = payrollItem(501L, run, otherEmployee, "10000.00", "1500.00", "8500.00");
        Payslip slip = payslip(600L, first, run, period);
        when(payrollRunRepository.findById(400L)).thenReturn(Optional.of(run));
        when(payrollItemRepository.findByPayrollRunIdOrderByIdAsc(400L)).thenReturn(List.of(first, second));
        when(payslipRepository.findByPayrollItemId(500L)).thenReturn(Optional.of(slip));

        PayrollPaymentRequest req = new PayrollPaymentRequest();
        req.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        req.setPaymentDate(LocalDate.of(2026, 4, 1));

        PayrollPaymentResponse payment = service.payPayroll(400L, req);
        assertEquals("PAY-000001", payment.getPaymentNumber());
        assertEquals(0, new BigDecimal("25500.00").compareTo(payment.getAmount()));
        assertEquals(PaymentMethod.BANK_TRANSFER, payment.getPaymentMethod());
        assertEquals("HR Officer", payment.getRecordedByName());

        verify(financeService).postTransaction(eq(TransactionType.TRANSFER),
                eq(new BigDecimal("25500.00")), eq(BusinessUnit.GENERAL),
                eq(SALARY_PAYABLE), eq(BANK), contains("Payroll disbursement"),
                eq("HR"), eq("HR_PAYROLL_PAYMENT"), any(),
                eq(PaymentMethod.BANK_TRANSFER), eq(LocalDate.of(2026, 4, 1)));
        verifyNoMoreInteractions(financeService);

        assertEquals(PayrollPaymentStatus.PAID, first.getPaymentStatus());
        assertEquals(PayrollItemStatus.PAID, first.getStatus());
        assertEquals(PayrollPaymentStatus.PAID, second.getPaymentStatus());
        assertEquals(PayrollPaymentStatus.PAID, slip.getPaymentStatus());
        verify(auditService).log(eq("PAYROLL_PAYMENT_POSTED"), eq("HR"), eq("PayrollPayment"),
                any(), isNull(), contains("PAY-000001"));
        verify(notificationService).createRoleNotification(eq("Payroll Disbursed"), any(), eq(Role.FINANCE));
    }

    @Test
    void cashPayrollPaymentSettlesFromCashAndCreditIsRejected() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.PROCESSED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.PROCESSED, "20000.00", "3000.00", "17000.00");
        PayrollItem item = payrollItem(500L, run, employee, "20000.00", "3000.00", "17000.00");
        when(payrollRunRepository.findById(400L)).thenReturn(Optional.of(run));
        when(payrollItemRepository.findByPayrollRunIdOrderByIdAsc(400L)).thenReturn(List.of(item));

        PayrollPaymentRequest cash = new PayrollPaymentRequest();
        cash.setPaymentMethod(PaymentMethod.CASH);
        service.payPayroll(400L, cash);
        verify(financeService).postTransaction(eq(TransactionType.TRANSFER),
                eq(new BigDecimal("17000.00")), eq(BusinessUnit.GENERAL),
                eq(SALARY_PAYABLE), eq(CASH), any(), eq("HR"), eq("HR_PAYROLL_PAYMENT"),
                any(), eq(PaymentMethod.CASH), any());

        PayrollPaymentRequest credit = new PayrollPaymentRequest();
        credit.setPaymentMethod(PaymentMethod.CREDIT);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.payPayroll(400L, credit));
        assertTrue(ex.getMessage().contains("cannot be disbursed on credit"));
    }

    @Test
    void perEmployeePartialPaymentLeavesTheRestOutstanding() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.PROCESSED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.PROCESSED, "20000.00", "3000.00", "17000.00");
        PayrollItem item = payrollItem(500L, run, employee, "20000.00", "3000.00", "17000.00");
        item.setStatus(PayrollItemStatus.PROCESSED);
        when(payrollRunRepository.findById(400L)).thenReturn(Optional.of(run));
        when(payrollItemRepository.findById(500L)).thenReturn(Optional.of(item));

        PayrollPaymentRequest req = new PayrollPaymentRequest();
        req.setPayrollItemId(500L);
        req.setAmount(new BigDecimal("7000"));

        assertEquals(0, new BigDecimal("7000.00").compareTo(service.payPayroll(400L, req).getAmount()));
        assertEquals(PayrollPaymentStatus.PARTIAL, item.getPaymentStatus());
        assertEquals(PayrollItemStatus.PROCESSED, item.getStatus());
        assertEquals(0, new BigDecimal("7000.00").compareTo(item.getPaidAmount()));
    }

    @Test
    void paymentCannotExceedOutstandingAndBatchMustSettleInFull() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.PROCESSED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.PROCESSED, "20000.00", "3000.00", "17000.00");
        PayrollItem item = payrollItem(500L, run, employee, "20000.00", "3000.00", "17000.00");
        when(payrollRunRepository.findById(400L)).thenReturn(Optional.of(run));
        when(payrollItemRepository.findByPayrollRunIdOrderByIdAsc(400L)).thenReturn(List.of(item));

        PayrollPaymentRequest tooMuch = new PayrollPaymentRequest();
        tooMuch.setAmount(new BigDecimal("20000"));
        BusinessException over = assertThrows(BusinessException.class, () -> service.payPayroll(400L, tooMuch));
        assertTrue(over.getMessage().contains("exceeds the outstanding net"));

        PayrollPaymentRequest partialBatch = new PayrollPaymentRequest();
        partialBatch.setAmount(new BigDecimal("5000"));
        BusinessException partial = assertThrows(BusinessException.class,
                () -> service.payPayroll(400L, partialBatch));
        assertTrue(partial.getMessage().contains("must settle the full outstanding net"));

        verifyNoInteractions(financeService);
        verify(payrollPaymentRepository, never()).saveAndFlush(any());
    }

    @Test
    void fullySettledRunHasNothingLeftToPay() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.PROCESSED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.PROCESSED, "20000.00", "3000.00", "17000.00");
        PayrollItem item = payrollItem(500L, run, employee, "20000.00", "3000.00", "17000.00");
        item.setPaidAmount(new BigDecimal("17000.00"));
        item.setPaymentStatus(PayrollPaymentStatus.PAID);
        when(payrollRunRepository.findById(400L)).thenReturn(Optional.of(run));
        when(payrollItemRepository.findByPayrollRunIdOrderByIdAsc(400L)).thenReturn(List.of(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.payPayroll(400L, new PayrollPaymentRequest()));
        assertTrue(ex.getMessage().contains("Nothing outstanding"));
        verifyNoInteractions(financeService);
    }

    @Test
    void paymentRequiresProcessedPayrollAndProcessAuthority() {
        setAuth(Permissions.HR_READ, Permissions.HR_PAYROLL);
        BusinessException denied = assertThrows(BusinessException.class,
                () -> service.payPayroll(400L, new PayrollPaymentRequest()));
        assertTrue(denied.getMessage().contains(Permissions.HR_PAYROLL_PROCESS));

        setAuth(Permissions.HR_PAYROLL_PROCESS);
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.APPROVED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.APPROVED, "20000.00", "3000.00", "17000.00");
        when(payrollRunRepository.findById(400L)).thenReturn(Optional.of(run));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.payPayroll(400L, new PayrollPaymentRequest()));
        assertTrue(ex.getMessage().contains("must be PROCESSED before it can be paid"));
        verifyNoInteractions(financeService);
    }

    // ─── Closing and cancelling ──────────────────────────────────

    @Test
    void closingPayrollIsAuthoritativeAndIdempotent() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.PROCESSED);
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));

        assertEquals(PayrollPeriodStatus.CLOSED, service.closePayroll(300L).getStatus());
        assertNotNull(period.getClosedAt());
        verify(auditService).log(eq("PAYROLL_CLOSED"), eq("HR"), eq("PayrollPeriod"),
                eq(300L), eq("PROCESSED"), eq("CLOSED"));

        assertEquals(PayrollPeriodStatus.CLOSED, service.closePayroll(300L).getStatus());
        verify(auditService, times(1)).log(eq("PAYROLL_CLOSED"), any(), any(), any(), any(), any());
    }

    @Test
    void closingRequiresPayrollAuthorityAndAProcessedPeriod() {
        setAuth(Permissions.HR_READ);
        assertThrows(BusinessException.class, () -> service.closePayroll(300L));

        setAuth(Permissions.HR_PAYROLL);
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.CALCULATED);
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        assertThrows(BusinessException.class, () -> service.closePayroll(300L));
    }

    @Test
    void cancellingPeriodCancelsItsRunsAndItems() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.CALCULATED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.CALCULATED, "20000.00", "3000.00", "17000.00");
        PayrollItem item = payrollItem(500L, run, employee, "20000.00", "3000.00", "17000.00");
        when(payrollPeriodRepository.findById(300L)).thenReturn(Optional.of(period));
        when(payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(300L)).thenReturn(List.of(run));
        when(payrollItemRepository.findByPayrollRunIdOrderByIdAsc(400L)).thenReturn(List.of(item));

        assertEquals(PayrollPeriodStatus.CANCELLED, service.cancelPayrollPeriod(300L).getStatus());
        assertEquals(PayrollRunStatus.CANCELLED, run.getStatus());
        assertEquals(PayrollItemStatus.CANCELLED, item.getStatus());
        verify(auditService).log(eq("PAYROLL_PERIOD_CANCELLED"), eq("HR"), eq("PayrollPeriod"),
                eq(300L), eq("CALCULATED"), eq("CANCELLED"));
        verifyNoInteractions(financeService);
    }

    // ─── Salary history ──────────────────────────────────────────

    @Test
    void salaryChangeAppendsHistoryAndAudits() {
        SalaryChangeRequest req = new SalaryChangeRequest();
        req.setBasicSalary(new BigDecimal("25000.00"));
        req.setEffectiveDate(LocalDate.of(2026, 4, 1));
        req.setCurrency("etb");
        req.setReason("Promotion");

        EmployeeResponse updated = service.changeSalary(100L, req);
        assertEquals(0, new BigDecimal("25000.00").compareTo(updated.getBasicSalary()));
        assertEquals("ETB", updated.getCurrency());

        ArgumentCaptor<EmployeeSalaryHistory> captor = ArgumentCaptor.forClass(EmployeeSalaryHistory.class);
        verify(salaryHistoryRepository).save(captor.capture());
        EmployeeSalaryHistory row = captor.getValue();
        assertEquals(employee, row.getEmployee());
        assertEquals(LocalDate.of(2026, 4, 1), row.getEffectiveDate());
        assertEquals(0, new BigDecimal("25000.00").compareTo(row.getBasicSalary()));
        assertEquals("Promotion", row.getReason());
        assertEquals(hrUser, row.getApprovedBy());
        verify(auditService).log(eq("SALARY_CHANGED"), eq("HR"), eq("EmployeeProfile"),
                eq(100L), eq("20000.00"), eq("25000.00"));
    }

    @Test
    void duplicateEffectiveDateAndNegativeSalaryAreRejectedOnSalaryChange() {
        SalaryChangeRequest negative = new SalaryChangeRequest();
        negative.setBasicSalary(new BigDecimal("-5"));
        assertThrows(BusinessException.class, () -> service.changeSalary(100L, negative));

        SalaryChangeRequest duplicate = new SalaryChangeRequest();
        duplicate.setBasicSalary(new BigDecimal("25000.00"));
        duplicate.setEffectiveDate(LocalDate.of(2026, 4, 1));
        when(salaryHistoryRepository.existsByEmployeeIdAndEffectiveDate(100L, LocalDate.of(2026, 4, 1)))
                .thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.changeSalary(100L, duplicate));
        assertTrue(ex.getMessage().contains("already exists for 2026-04-01"));
        assertEquals(0, new BigDecimal("20000.00").compareTo(employee.getBasicSalary()));
        verify(salaryHistoryRepository, never()).save(any());
    }

    @Test
    void salaryHistoryIsPayrollOperatorsOnly() {
        when(salaryHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(100L)).thenReturn(List.of());
        assertTrue(service.listSalaryHistory(100L).isEmpty());

        setAuth(Permissions.HR_READ, Permissions.HR_UPDATE);
        assertThrows(BusinessException.class, () -> service.listSalaryHistory(100L));
    }

    // ─── Self-service isolation ──────────────────────────────────

    @Test
    void selfServiceResolvesOnlyTheCallersOwnRecord() {
        setAuthAs("staff@test.com", Permissions.HR_SELF);

        EmployeeResponse me = service.myProfile();
        assertEquals(100L, me.getId());
        assertEquals("EMP-000001", me.getEmployeeNumber());
        // Own compensation is always visible, even without payroll authority.
        assertEquals(0, new BigDecimal("20000.00").compareTo(me.getBasicSalary()));

        when(payslipRepository.findByEmployeeIdOrderByIdDesc(100L)).thenReturn(List.of());
        assertTrue(service.myPayslips().isEmpty());
        verify(employeeRepository, never()).findById(101L);
    }

    @Test
    void selfServiceFailsWithoutALinkedEmployeeRecordOrAuthentication() {
        setAuthAs("unlinked@test.com", Permissions.HR_SELF);
        assertThrows(ResourceNotFoundException.class, () -> service.myProfile());

        SecurityContextHolder.clearContext();
        BusinessException ex = assertThrows(BusinessException.class, () -> service.myProfile());
        assertTrue(ex.getMessage().contains("No authenticated user"));
    }

    @Test
    void payslipIsVisibleToItsOwnerAndHrButNotToOtherEmployees() {
        PayrollPeriod period = payrollPeriod(PayrollPeriodStatus.PROCESSED);
        PayrollRun run = payrollRun(period, PayrollRunStatus.PROCESSED, "20000.00", "3000.00", "17000.00");
        PayrollItem item = payrollItem(500L, run, employee, "20000.00", "3000.00", "17000.00");
        Payslip slip = payslip(600L, item, run, period);
        when(payslipRepository.findById(600L)).thenReturn(Optional.of(slip));

        setAuthAs("staff@test.com", Permissions.HR_SELF);
        PayslipResponse own = service.getPayslip(600L);
        assertEquals("PS-000600", own.getPayslipNumber());
        assertEquals(0, new BigDecimal("17000.00").compareTo(own.getNetSalary()));

        setAuthAs("field@test.com", Permissions.HR_SELF);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getPayslip(600L));
        assertTrue(ex.getMessage().contains("Not authorized to view this payslip"));

        setAuth(Permissions.HR_READ);
        assertEquals("PS-000600", service.getPayslip(600L).getPayslipNumber());

        setAuth(Permissions.HR_PAYROLL);
        assertEquals("PS-000600", service.getPayslip(600L).getPayslipNumber());
    }

    @Test
    void consultantWithSelfServiceOnlyIsLockedOutOfAdminHrOperations() {
        setAuthAs("field@test.com", Permissions.HR_SELF, Permissions.CONSULTANCY_READ);

        assertThrows(BusinessException.class, () -> service.listSalaryHistory(100L));
        assertThrows(BusinessException.class, () -> service.approvePayroll(300L));
        assertThrows(BusinessException.class, () -> service.processPayroll(300L));
        assertThrows(BusinessException.class, () -> service.closePayroll(300L));
        assertThrows(BusinessException.class, () -> service.cancelPayrollPeriod(300L));
        assertThrows(BusinessException.class, () -> service.approveLeaveRequest(200L));
        assertThrows(BusinessException.class, () -> service.rejectLeaveRequest(200L, new LeaveDecisionRequest()));
        assertThrows(BusinessException.class, () -> service.payPayroll(400L, new PayrollPaymentRequest()));

        EmployeeStatusRequest terminate = new EmployeeStatusRequest();
        terminate.setStatus(EmploymentStatus.TERMINATED);
        assertThrows(BusinessException.class, () -> service.changeEmployeeStatus(100L, terminate));

        assertFalse(service.canSeeCompensation());
        verifyNoInteractions(financeService);
        verifyNoInteractions(auditService);
        verifyNoInteractions(notificationService);
        verify(employeeRepository, never()).save(any());
    }

    // ─── Fixtures & helpers ──────────────────────────────────────

    private User user(Long id, String email, String fullName) {
        User u = new User();
        setId(u, id);
        u.setEmail(email);
        u.setFullName(fullName);
        return u;
    }

    private EmployeeProfile employeeProfile(Long id, String number, String first, String last,
                                            String salary, User linked) {
        EmployeeProfile e = new EmployeeProfile();
        setId(e, id);
        e.setEmployeeNumber(number);
        e.setFirstName(first);
        e.setLastName(last);
        e.setBasicSalary(new BigDecimal(salary));
        e.setCurrency("ETB");
        e.setEmploymentStatus(EmploymentStatus.ACTIVE);
        e.setEmploymentType(EmploymentType.FULL_TIME);
        e.setHireDate(LocalDate.of(2026, 1, 15));
        e.setDepartment(department);
        e.setPosition(position);
        e.setBankName("Commercial Bank");
        e.setBankAccountRef("****1234");
        e.setUser(linked);
        return e;
    }

    private EmployeeRequest employeeRequest(String first, String last, String salary) {
        EmployeeRequest req = new EmployeeRequest();
        req.setFirstName(first);
        req.setLastName(last);
        req.setBasicSalary(new BigDecimal(salary));
        req.setHireDate(LocalDate.of(2026, 1, 15));
        req.setEmploymentType(EmploymentType.FULL_TIME);
        req.setDepartmentId(10L);
        req.setPositionId(11L);
        req.setCurrency("ETB");
        return req;
    }

    private AttendanceRequest attendanceRequest(LocalDate day, LocalTime in, LocalTime out) {
        AttendanceRequest req = new AttendanceRequest();
        req.setEmployeeId(100L);
        req.setAttendanceDate(day);
        req.setCheckIn(in);
        req.setCheckOut(out);
        req.setStatus(AttendanceStatus.PRESENT);
        return req;
    }

    private LeaveRequestCreateRequest leaveRequest(Long employeeId, LocalDate start, LocalDate end) {
        LeaveRequestCreateRequest req = new LeaveRequestCreateRequest();
        req.setEmployeeId(employeeId);
        req.setLeaveTypeId(20L);
        req.setStartDate(start);
        req.setEndDate(end);
        req.setReason("Family matters");
        return req;
    }

    private LeaveBalance leaveBalance(EmployeeProfile emp, String entitlement, String used) {
        LeaveBalance b = new LeaveBalance();
        setId(b, 250L);
        b.setEmployee(emp);
        b.setLeaveType(annualLeave);
        b.setYear(2026);
        b.setEntitlement(new BigDecimal(entitlement));
        b.setUsedDays(new BigDecimal(used));
        b.recalculateRemaining();
        return b;
    }

    private LeaveRequest pendingLeave(Long id, EmployeeProfile emp, String days) {
        LeaveRequest r = new LeaveRequest();
        setId(r, id);
        r.setRequestNumber("LV-000010");
        r.setEmployee(emp);
        r.setLeaveType(annualLeave);
        r.setStartDate(LocalDate.of(2026, 3, 2));
        r.setEndDate(LocalDate.of(2026, 3, 6));
        r.setTotalDays(new BigDecimal(days));
        r.setStatus(LeaveRequestStatus.PENDING);
        return r;
    }

    private PayrollDeductionType deductionType(Long id, String code, DeductionCategory category, String rate) {
        PayrollDeductionType d = new PayrollDeductionType();
        setId(d, id);
        d.setCode(code);
        d.setName(code);
        d.setCategory(category);
        d.setRatePercent(new BigDecimal(rate));
        return d;
    }

    private PayrollDeductionType fixedDeductionType(Long id, String code, DeductionCategory category, String amount) {
        PayrollDeductionType d = new PayrollDeductionType();
        setId(d, id);
        d.setCode(code);
        d.setName(code);
        d.setCategory(category);
        d.setFixedAmount(new BigDecimal(amount));
        return d;
    }

    private PayrollPeriod payrollPeriod(PayrollPeriodStatus status) {
        PayrollPeriod p = new PayrollPeriod();
        setId(p, 300L);
        p.setPeriodCode("PR-2026-03");
        p.setStartDate(LocalDate.of(2026, 3, 1));
        p.setEndDate(LocalDate.of(2026, 3, 31));
        p.setStatus(status);
        return p;
    }

    private PayrollRun payrollRun(PayrollPeriod period, PayrollRunStatus status,
                                  String gross, String deductions, String net) {
        PayrollRun r = new PayrollRun();
        setId(r, 400L);
        r.setPayrollPeriod(period);
        r.setRunNumber(1);
        r.setStatus(status);
        r.setEmployeeCount(1);
        r.setGrossAmount(new BigDecimal(gross));
        r.setDeductionAmount(new BigDecimal(deductions));
        r.setNetAmount(new BigDecimal(net));
        return r;
    }

    private PayrollItem payrollItem(Long id, PayrollRun run, EmployeeProfile emp,
                                    String gross, String deductions, String net) {
        PayrollItem i = new PayrollItem();
        setId(i, id);
        i.setPayrollRun(run);
        i.setEmployee(emp);
        i.setBasicSalary(new BigDecimal(gross));
        i.setGrossSalary(new BigDecimal(gross));
        i.setTotalDeductions(new BigDecimal(deductions));
        i.setTax(new BigDecimal(deductions));
        i.setNetSalary(new BigDecimal(net));
        i.setPaidAmount(new BigDecimal("0.00"));
        i.setStatus(PayrollItemStatus.CALCULATED);
        i.setPaymentStatus(PayrollPaymentStatus.PENDING);
        return i;
    }

    private Payslip payslip(Long id, PayrollItem item, PayrollRun run, PayrollPeriod period) {
        Payslip p = new Payslip();
        setId(p, id);
        p.setPayslipNumber(String.format("PS-%06d", id));
        p.setPayrollItem(item);
        p.setEmployee(item.getEmployee());
        p.setRun(run);
        p.setPeriod(period);
        p.setPeriodStartDate(period.getStartDate());
        p.setPeriodEndDate(period.getEndDate());
        p.setBasicSalary(item.getBasicSalary());
        p.setGrossSalary(item.getGrossSalary());
        p.setTotalDeductions(item.getTotalDeductions());
        p.setNetSalary(item.getNetSalary());
        return p;
    }

    private void setAuth(String... authorities) {
        setAuthAs("hr@test.com", authorities);
    }

    private void setAuthAs(String principal, String... authorities) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(principal, "x",
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private PayrollRun withRunId(PayrollRun run) {
        return withRunId(run, 400L);
    }

    private PayrollRun withRunId(PayrollRun run, Long id) {
        if (run == null) {
            return null;
        }
        if (run.getId() == null) {
            setId(run, id);
        }
        return run;
    }

    private <T> T withId(T entity) {
        if (entity == null) {
            return null;
        }
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            if (field.get(entity) == null) {
                field.set(entity, ids.getAndIncrement());
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return entity;
    }

    private static void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
