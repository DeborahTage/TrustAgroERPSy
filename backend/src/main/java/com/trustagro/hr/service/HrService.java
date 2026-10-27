package com.trustagro.hr.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.finance.entity.AccountCode;
import com.trustagro.finance.entity.BusinessUnit;
import com.trustagro.finance.entity.FinanceSourceTypes;
import com.trustagro.finance.entity.FinanceTransaction;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Single backend-authoritative HR service: employee records, attendance, leave and payroll.
 * <p>
 * Payroll has no ledger of its own — every accounting effect is posted through
 * {@link FinanceService} against the shared chart of accounts, keyed by
 * {@code (MODULE_HR, referenceType, referenceId)} so repeated calls cannot double-post.
 * Deduction rates are configuration ({@link PayrollDeductionType}); no statutory
 * schedule is encoded anywhere in this module.
 */
@Service
@RequiredArgsConstructor
public class HrService {

    public static final String MODULE = "HR";
    public static final String SOURCE_MODULE = FinanceSourceTypes.MODULE_HR;

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final String SEQ_EMPLOYEE = "EMPLOYEE";
    private static final String SEQ_LEAVE = "LEAVE";
    private static final String SEQ_PAYROLL_PERIOD = "PAYROLL_PERIOD";
    private static final String SEQ_PAYROLL_PAYMENT = "PAYROLL_PAYMENT";
    private static final String SEQ_PAYSLIP = "PAYSLIP";

    private final HrDepartmentRepository departmentRepository;
    private final HrPositionRepository positionRepository;
    private final EmployeeProfileRepository employeeRepository;
    private final EmployeeSalaryHistoryRepository salaryHistoryRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final PayrollDeductionTypeRepository deductionTypeRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollItemRepository payrollItemRepository;
    private final PayslipRepository payslipRepository;
    private final PayrollPaymentRepository payrollPaymentRepository;
    private final HrNumberSequenceRepository sequenceRepository;
    private final UserRepository userRepository;
    private final FinanceService financeService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ─── Access control ──────────────────────────────────────────

    private Set<String> authorities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return Set.of();
        }
        Set<String> set = new HashSet<>();
        for (GrantedAuthority ga : auth.getAuthorities()) {
            set.add(ga.getAuthority());
        }
        return set;
    }

    private boolean hasAuthority(String authority) {
        return authorities().contains(authority);
    }

    private void assertHasAuthority(String authority) {
        if (!hasAuthority(authority)) {
            throw new BusinessException("Not authorized: requires " + authority);
        }
    }

    /**
     * Compensation is only visible to payroll operators, HR record owners, and
     * Finance staff who already hold HR read access.
     */
    public boolean canSeeCompensation() {
        Set<String> auth = authorities();
        return auth.contains(Permissions.HR_PAYROLL)
                || auth.contains(Permissions.HR_UPDATE)
                || (auth.contains(Permissions.FINANCE_READ) && auth.contains(Permissions.HR_READ));
    }

    private User currentUser() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    // ─── Departments ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments(boolean activeOnly) {
        List<HrDepartment> list = activeOnly
                ? departmentRepository.findByActiveTrueOrderByNameAsc()
                : departmentRepository.findAllByOrderByNameAsc();
        return list.stream().map(this::toDepartmentResponse).toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getDepartment(Long id) {
        return toDepartmentResponse(requireDepartment(id));
    }

    @Transactional
    public DepartmentResponse createDepartment(DepartmentRequest req) {
        String code = normalizeCode(req.getCode(), req.getName());
        if (departmentRepository.existsByCode(code)) {
            throw new BusinessException("Department code already exists: " + code);
        }
        HrDepartment d = new HrDepartment();
        d.setCode(code);
        d.setName(req.getName().trim());
        d.setDescription(req.getDescription());
        d.setActive(req.getActive() == null || req.getActive());
        HrDepartment saved = departmentRepository.save(d);
        auditService.log("HR_DEPARTMENT_CREATED", MODULE, "HrDepartment", saved.getId(), null, saved.getCode());
        return toDepartmentResponse(saved);
    }

    @Transactional
    public DepartmentResponse updateDepartment(Long id, DepartmentRequest req) {
        HrDepartment d = requireDepartment(id);
        String previous = d.getCode();
        if (req.getCode() != null && !req.getCode().isBlank()) {
            String code = normalizeCode(req.getCode(), req.getName());
            if (!code.equals(d.getCode()) && departmentRepository.existsByCode(code)) {
                throw new BusinessException("Department code already exists: " + code);
            }
            d.setCode(code);
        }
        if (req.getName() != null && !req.getName().isBlank()) {
            d.setName(req.getName().trim());
        }
        if (req.getDescription() != null) {
            d.setDescription(req.getDescription());
        }
        if (req.getActive() != null) {
            d.setActive(req.getActive());
        }
        HrDepartment saved = departmentRepository.save(d);
        auditService.log("HR_DEPARTMENT_UPDATED", MODULE, "HrDepartment", saved.getId(), previous, saved.getCode());
        return toDepartmentResponse(saved);
    }

    // ─── Positions ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PositionResponse> listPositions(boolean activeOnly) {
        List<HrPosition> list = activeOnly
                ? positionRepository.findByActiveTrueOrderByNameAsc()
                : positionRepository.findAllByOrderByNameAsc();
        return list.stream().map(this::toPositionResponse).toList();
    }

    @Transactional(readOnly = true)
    public PositionResponse getPosition(Long id) {
        return toPositionResponse(requirePosition(id));
    }

    @Transactional
    public PositionResponse createPosition(PositionRequest req) {
        String code = normalizeCode(req.getCode(), req.getName());
        if (positionRepository.existsByCode(code)) {
            throw new BusinessException("Position code already exists: " + code);
        }
        HrPosition p = new HrPosition();
        p.setCode(code);
        p.setName(req.getName().trim());
        p.setDescription(req.getDescription());
        p.setActive(req.getActive() == null || req.getActive());
        HrPosition saved = positionRepository.save(p);
        auditService.log("HR_POSITION_CREATED", MODULE, "HrPosition", saved.getId(), null, saved.getCode());
        return toPositionResponse(saved);
    }

    @Transactional
    public PositionResponse updatePosition(Long id, PositionRequest req) {
        HrPosition p = requirePosition(id);
        String previous = p.getCode();
        if (req.getCode() != null && !req.getCode().isBlank()) {
            String code = normalizeCode(req.getCode(), req.getName());
            if (!code.equals(p.getCode()) && positionRepository.existsByCode(code)) {
                throw new BusinessException("Position code already exists: " + code);
            }
            p.setCode(code);
        }
        if (req.getName() != null && !req.getName().isBlank()) {
            p.setName(req.getName().trim());
        }
        if (req.getDescription() != null) {
            p.setDescription(req.getDescription());
        }
        if (req.getActive() != null) {
            p.setActive(req.getActive());
        }
        HrPosition saved = positionRepository.save(p);
        auditService.log("HR_POSITION_UPDATED", MODULE, "HrPosition", saved.getId(), previous, saved.getCode());
        return toPositionResponse(saved);
    }

    // ─── Employees ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listEmployees(EmploymentStatus status, Long departmentId, String search) {
        List<EmployeeProfile> list;
        if (status != null) {
            list = employeeRepository.findByEmploymentStatus(status);
        } else if (departmentId != null) {
            list = employeeRepository.findByDepartmentId(departmentId);
        } else {
            list = employeeRepository.findAllOrdered();
        }
        if (departmentId != null && status != null) {
            list = list.stream().filter(e -> e.getDepartment() != null
                    && departmentId.equals(e.getDepartment().getId())).toList();
        }
        if (search != null && !search.isBlank()) {
            String needle = search.trim().toLowerCase();
            list = list.stream().filter(e -> e.getFullName().toLowerCase().contains(needle)
                    || e.getEmployeeNumber().toLowerCase().contains(needle)).toList();
        }
        boolean showSalary = canSeeCompensation();
        return list.stream().map(e -> toEmployeeResponse(e, showSalary)).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(Long id) {
        return toEmployeeResponse(requireEmployee(id), canSeeCompensation());
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest req) {
        BigDecimal salary = nvl(req.getBasicSalary());
        if (salary.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Basic salary cannot be negative");
        }
        EmployeeProfile e = new EmployeeProfile();
        e.setEmployeeNumber(nextEmployeeNumber());
        applyEmployeeFields(e, req);
        e.setBasicSalary(salary);
        e.setEmploymentStatus(EmploymentStatus.ACTIVE);
        if (req.getUserId() != null) {
            e.setUser(requireUnlinkedUser(req.getUserId(), null));
        }
        EmployeeProfile saved = employeeRepository.save(e);

        if (salary.compareTo(BigDecimal.ZERO) > 0) {
            recordSalaryHistory(saved, salary, saved.getCurrency(),
                    saved.getHireDate() != null ? saved.getHireDate() : LocalDate.now(),
                    "Initial salary on hire");
        }

        auditService.log("EMPLOYEE_CREATED", MODULE, "EmployeeProfile", saved.getId(), null, saved.getEmployeeNumber());
        notificationService.createRoleNotification("New Employee Record",
                String.format("Employee %s (%s) was created", saved.getFullName(), saved.getEmployeeNumber()),
                Role.HR);
        return toEmployeeResponse(saved, canSeeCompensation());
    }

    @Transactional
    public EmployeeResponse updateEmployee(Long id, EmployeeRequest req) {
        EmployeeProfile e = requireEmployee(id);
        applyEmployeeFields(e, req);
        if (req.getUserId() != null) {
            e.setUser(requireUnlinkedUser(req.getUserId(), e.getId()));
        }
        // Salary is deliberately not editable here — it moves only through changeSalary()
        // so that every change leaves a row in the salary history.
        EmployeeProfile saved = employeeRepository.save(e);
        auditService.log("EMPLOYEE_UPDATED", MODULE, "EmployeeProfile", saved.getId(), null, saved.getEmployeeNumber());
        return toEmployeeResponse(saved, canSeeCompensation());
    }

    @Transactional
    public EmployeeResponse changeEmployeeStatus(Long id, EmployeeStatusRequest req) {
        EmployeeProfile e = requireEmployee(id);
        EmploymentStatus previous = e.getEmploymentStatus();
        EmploymentStatus next = req.getStatus();
        if (previous == next) {
            return toEmployeeResponse(e, canSeeCompensation());
        }
        boolean exiting = next == EmploymentStatus.TERMINATED || next == EmploymentStatus.RESIGNED;
        if (exiting) {
            assertHasAuthority(Permissions.HR_TERMINATE);
            e.setTerminationDate(req.getEffectiveDate() != null ? req.getEffectiveDate() : LocalDate.now());
            e.setTerminationReason(req.getReason());
        }
        e.setEmploymentStatus(next);
        EmployeeProfile saved = employeeRepository.save(e);

        auditService.log(exiting ? "EMPLOYEE_TERMINATED" : "EMPLOYEE_STATUS_CHANGED", MODULE,
                "EmployeeProfile", saved.getId(), previous.name(), next.name());
        notificationService.createRoleNotification("Employee Status Changed",
                String.format("%s (%s): %s → %s", saved.getFullName(), saved.getEmployeeNumber(), previous, next),
                Role.HR);
        return toEmployeeResponse(saved, canSeeCompensation());
    }

    @Transactional(readOnly = true)
    public List<SalaryHistoryResponse> listSalaryHistory(Long employeeId) {
        assertHasAuthority(Permissions.HR_PAYROLL);
        EmployeeProfile e = requireEmployee(employeeId);
        return salaryHistoryRepository.findByEmployeeIdOrderByEffectiveDateDesc(e.getId())
                .stream().map(this::toSalaryHistoryResponse).toList();
    }

    @Transactional
    public EmployeeResponse changeSalary(Long employeeId, SalaryChangeRequest req) {
        EmployeeProfile e = requireEmployee(employeeId);
        BigDecimal salary = nvl(req.getBasicSalary());
        if (salary.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Basic salary cannot be negative");
        }
        LocalDate effective = req.getEffectiveDate() != null ? req.getEffectiveDate() : LocalDate.now();
        String currency = req.getCurrency() != null && !req.getCurrency().isBlank()
                ? req.getCurrency().trim().toUpperCase() : e.getCurrency();
        BigDecimal previous = nvl(e.getBasicSalary());

        recordSalaryHistory(e, salary, currency, effective, req.getReason());
        e.setBasicSalary(salary);
        e.setCurrency(currency);
        EmployeeProfile saved = employeeRepository.save(e);

        auditService.log("SALARY_CHANGED", MODULE, "EmployeeProfile", saved.getId(),
                previous.toPlainString(), salary.toPlainString());
        return toEmployeeResponse(saved, canSeeCompensation());
    }

    private void recordSalaryHistory(EmployeeProfile employee, BigDecimal salary, String currency,
                                     LocalDate effectiveDate, String reason) {
        if (salaryHistoryRepository.existsByEmployeeIdAndEffectiveDate(employee.getId(), effectiveDate)) {
            throw new BusinessException("A salary record already exists for " + effectiveDate
                    + ". Use a different effective date.");
        }
        EmployeeSalaryHistory h = new EmployeeSalaryHistory();
        h.setEmployee(employee);
        h.setEffectiveDate(effectiveDate);
        h.setBasicSalary(scale(salary));
        h.setCurrency(currency);
        h.setReason(reason);
        h.setApprovedBy(currentUser());
        salaryHistoryRepository.save(h);
    }

    private void applyEmployeeFields(EmployeeProfile e, EmployeeRequest req) {
        if (req.getFirstName() != null && !req.getFirstName().isBlank()) {
            e.setFirstName(req.getFirstName().trim());
        }
        if (req.getLastName() != null && !req.getLastName().isBlank()) {
            e.setLastName(req.getLastName().trim());
        }
        if (req.getMiddleName() != null) e.setMiddleName(req.getMiddleName());
        if (req.getPhone() != null) e.setPhone(req.getPhone());
        if (req.getEmail() != null) e.setEmail(req.getEmail());
        if (req.getAddress() != null) e.setAddress(req.getAddress());
        if (req.getHireDate() != null) e.setHireDate(req.getHireDate());
        if (req.getEmploymentType() != null) e.setEmploymentType(req.getEmploymentType());
        if (req.getCurrency() != null && !req.getCurrency().isBlank()) {
            e.setCurrency(req.getCurrency().trim().toUpperCase());
        }
        if (req.getBankName() != null) e.setBankName(req.getBankName());
        if (req.getBankAccountRef() != null) e.setBankAccountRef(req.getBankAccountRef());
        if (req.getEmergencyContact() != null) e.setEmergencyContact(req.getEmergencyContact());
        if (req.getNotes() != null) e.setNotes(req.getNotes());
        if (req.getDepartmentId() != null) e.setDepartment(requireDepartment(req.getDepartmentId()));
        if (req.getPositionId() != null) e.setPosition(requirePosition(req.getPositionId()));
        if (req.getManagerId() != null) {
            if (e.getId() != null && req.getManagerId().equals(e.getId())) {
                throw new BusinessException("An employee cannot be their own manager");
            }
            e.setManager(requireEmployee(req.getManagerId()));
        }
    }

    private User requireUnlinkedUser(Long userId, Long allowedEmployeeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Optional<EmployeeProfile> linked = employeeRepository.findByUserId(userId);
        if (linked.isPresent() && !linked.get().getId().equals(allowedEmployeeId)) {
            throw new BusinessException("User is already linked to employee "
                    + linked.get().getEmployeeNumber());
        }
        return user;
    }

    // ─── Attendance ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AttendanceResponse> listAttendance(Long employeeId, LocalDate from, LocalDate to) {
        List<AttendanceRecord> list;
        if (employeeId != null && from != null && to != null) {
            list = attendanceRepository.findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
                    employeeId, from, to);
        } else if (employeeId != null) {
            list = attendanceRepository.findByEmployeeIdOrderByAttendanceDateDesc(employeeId);
        } else if (from != null && to != null) {
            list = attendanceRepository.findByAttendanceDateBetweenOrderByAttendanceDateDesc(from, to);
        } else {
            LocalDate day = from != null ? from : LocalDate.now();
            list = attendanceRepository.findByAttendanceDateOrderByEmployeeIdAsc(day);
        }
        return list.stream().map(this::toAttendanceResponse).toList();
    }

    @Transactional(readOnly = true)
    public AttendanceResponse getAttendance(Long id) {
        return toAttendanceResponse(requireAttendance(id));
    }

    @Transactional
    public AttendanceResponse recordAttendance(AttendanceRequest req) {
        EmployeeProfile employee = requireEmployee(req.getEmployeeId());
        if (attendanceRepository.existsByEmployeeIdAndAttendanceDate(employee.getId(), req.getAttendanceDate())) {
            throw new BusinessException("Attendance for " + employee.getEmployeeNumber()
                    + " on " + req.getAttendanceDate() + " already exists");
        }
        AttendanceRecord a = new AttendanceRecord();
        a.setEmployee(employee);
        a.setAttendanceDate(req.getAttendanceDate());
        applyAttendanceFields(a, req);
        a.setRecordedBy(currentUser());
        AttendanceRecord saved = attendanceRepository.save(a);
        auditService.log("ATTENDANCE_CREATED", MODULE, "AttendanceRecord", saved.getId(),
                null, employee.getEmployeeNumber() + "@" + saved.getAttendanceDate());
        return toAttendanceResponse(saved);
    }

    @Transactional
    public AttendanceResponse updateAttendance(Long id, AttendanceRequest req) {
        AttendanceRecord a = requireAttendance(id);
        String previous = a.getStatus() + "/" + a.getHoursWorked();
        applyAttendanceFields(a, req);
        AttendanceRecord saved = attendanceRepository.save(a);
        auditService.log("ATTENDANCE_UPDATED", MODULE, "AttendanceRecord", saved.getId(),
                previous, saved.getStatus() + "/" + saved.getHoursWorked());
        return toAttendanceResponse(saved);
    }

    private void applyAttendanceFields(AttendanceRecord a, AttendanceRequest req) {
        a.setCheckIn(req.getCheckIn());
        a.setCheckOut(req.getCheckOut());
        if (req.getStatus() != null) {
            a.setStatus(req.getStatus());
        }
        if (req.getNotes() != null) {
            a.setNotes(req.getNotes());
        }
        a.setHoursWorked(calculateHoursWorked(req.getCheckIn(), req.getCheckOut()));
    }

    /** Overnight shifts are supported: a checkout before checkin rolls into the next day. */
    BigDecimal calculateHoursWorked(LocalTime checkIn, LocalTime checkOut) {
        if (checkIn == null || checkOut == null) {
            return ZERO;
        }
        long minutes = Duration.between(checkIn, checkOut).toMinutes();
        if (minutes < 0) {
            minutes += Duration.ofDays(1).toMinutes();
        }
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    // ─── Leave types ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> listLeaveTypes(boolean activeOnly) {
        List<LeaveType> list = activeOnly
                ? leaveTypeRepository.findByActiveTrueOrderByNameAsc()
                : leaveTypeRepository.findAllByOrderByNameAsc();
        return list.stream().map(this::toLeaveTypeResponse).toList();
    }

    @Transactional
    public LeaveTypeResponse createLeaveType(LeaveTypeRequest req) {
        String code = normalizeCode(req.getCode(), req.getName());
        if (leaveTypeRepository.existsByCode(code)) {
            throw new BusinessException("Leave type code already exists: " + code);
        }
        LeaveType t = new LeaveType();
        t.setCode(code);
        applyLeaveTypeFields(t, req);
        LeaveType saved = leaveTypeRepository.save(t);
        auditService.log("HR_LEAVE_TYPE_CREATED", MODULE, "LeaveType", saved.getId(), null, saved.getCode());
        return toLeaveTypeResponse(saved);
    }

    @Transactional
    public LeaveTypeResponse updateLeaveType(Long id, LeaveTypeRequest req) {
        LeaveType t = requireLeaveType(id);
        applyLeaveTypeFields(t, req);
        LeaveType saved = leaveTypeRepository.save(t);
        auditService.log("HR_LEAVE_TYPE_UPDATED", MODULE, "LeaveType", saved.getId(), null, saved.getCode());
        return toLeaveTypeResponse(saved);
    }

    private void applyLeaveTypeFields(LeaveType t, LeaveTypeRequest req) {
        if (req.getName() != null && !req.getName().isBlank()) {
            t.setName(req.getName().trim());
        }
        if (req.getDescription() != null) t.setDescription(req.getDescription());
        if (req.getBalanceControlled() != null) t.setBalanceControlled(req.getBalanceControlled());
        if (req.getDefaultAnnualEntitlement() != null) {
            if (req.getDefaultAnnualEntitlement().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Annual entitlement cannot be negative");
            }
            t.setDefaultAnnualEntitlement(scale(req.getDefaultAnnualEntitlement()));
        }
        if (req.getPaid() != null) t.setPaid(req.getPaid());
        if (req.getActive() != null) t.setActive(req.getActive());
    }

    // ─── Leave requests ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> listLeaveRequests(Long employeeId, LeaveRequestStatus status) {
        List<LeaveRequest> list;
        if (employeeId != null && status != null) {
            list = leaveRequestRepository.findByEmployeeIdAndStatusOrderByCreatedAtDesc(employeeId, status);
        } else if (employeeId != null) {
            list = leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        } else if (status != null) {
            list = leaveRequestRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = leaveRequestRepository.findAll();
        }
        return list.stream()
                .sorted(Comparator.comparing(LeaveRequest::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toLeaveRequestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeaveRequestResponse getLeaveRequest(Long id) {
        return toLeaveRequestResponse(requireLeaveRequest(id));
    }

    @Transactional
    public LeaveRequestResponse createLeaveRequest(LeaveRequestCreateRequest req, boolean selfService) {
        EmployeeProfile employee;
        if (selfService) {
            employee = requireOwnEmployeeProfile();
        } else {
            if (req.getEmployeeId() == null) {
                throw new BusinessException("employeeId is required when filing leave on behalf of an employee");
            }
            employee = requireEmployee(req.getEmployeeId());
        }
        LeaveType type = requireLeaveType(req.getLeaveTypeId());
        if (!type.isActive()) {
            throw new BusinessException("Leave type is not active: " + type.getCode());
        }
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BusinessException("Leave end date cannot precede the start date");
        }
        BigDecimal totalDays = calculateLeaveDays(req.getStartDate(), req.getEndDate());

        if (type.isBalanceControlled()) {
            LeaveBalance balance = getOrCreateBalance(employee, type, req.getStartDate().getYear());
            if (nvl(balance.getRemainingDays()).compareTo(totalDays) < 0) {
                throw new BusinessException(String.format(
                        "Insufficient %s balance: %s day(s) remaining, %s requested",
                        type.getCode(), nvl(balance.getRemainingDays()).toPlainString(), totalDays.toPlainString()));
            }
        }

        LeaveRequest r = new LeaveRequest();
        r.setRequestNumber(nextLeaveNumber());
        r.setEmployee(employee);
        r.setLeaveType(type);
        r.setStartDate(req.getStartDate());
        r.setEndDate(req.getEndDate());
        r.setTotalDays(totalDays);
        r.setReason(req.getReason());
        r.setStatus(LeaveRequestStatus.PENDING);
        r.setCreatedBy(currentUser());
        LeaveRequest saved = leaveRequestRepository.save(r);

        auditService.log("LEAVE_REQUESTED", MODULE, "LeaveRequest", saved.getId(), null, saved.getRequestNumber());
        notificationService.createRoleNotification("Leave Request Submitted",
                String.format("%s (%s) requested %s day(s) of %s from %s",
                        employee.getFullName(), employee.getEmployeeNumber(),
                        totalDays.toPlainString(), type.getName(), req.getStartDate()),
                Role.HR);
        return toLeaveRequestResponse(saved);
    }

    @Transactional
    public LeaveRequestResponse approveLeaveRequest(Long id) {
        assertHasAuthority(Permissions.HR_LEAVE_APPROVE);
        LeaveRequest r = requireLeaveRequest(id);
        LeaveLifecycle.assertTransition(r.getStatus(), LeaveRequestStatus.APPROVED);

        User approver = currentUser();
        if (approver != null && r.getEmployee().getUser() != null
                && approver.getId().equals(r.getEmployee().getUser().getId())) {
            throw new BusinessException("You cannot approve your own leave request");
        }

        LeaveType type = r.getLeaveType();
        if (type.isBalanceControlled()) {
            LeaveBalance balance = getOrCreateBalance(r.getEmployee(), type, r.getStartDate().getYear());
            BigDecimal days = nvl(r.getTotalDays());
            if (nvl(balance.getRemainingDays()).compareTo(days) < 0) {
                throw new BusinessException(String.format(
                        "Insufficient %s balance: %s day(s) remaining, %s requested",
                        type.getCode(), nvl(balance.getRemainingDays()).toPlainString(), days.toPlainString()));
            }
            balance.setUsedDays(nvl(balance.getUsedDays()).add(days));
            balance.recalculateRemaining();
            leaveBalanceRepository.save(balance);
        }

        r.setStatus(LeaveRequestStatus.APPROVED);
        r.setApprovedBy(approver);
        r.setApprovedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(r);

        auditService.log("LEAVE_APPROVED", MODULE, "LeaveRequest", saved.getId(),
                LeaveRequestStatus.PENDING.name(), LeaveRequestStatus.APPROVED.name());
        notifyEmployee(saved.getEmployee(), "Leave Approved",
                String.format("Your %s request %s (%s → %s) was approved",
                        saved.getLeaveType().getName(), saved.getRequestNumber(),
                        saved.getStartDate(), saved.getEndDate()));
        return toLeaveRequestResponse(saved);
    }

    @Transactional
    public LeaveRequestResponse rejectLeaveRequest(Long id, LeaveDecisionRequest body) {
        assertHasAuthority(Permissions.HR_LEAVE_APPROVE);
        LeaveRequest r = requireLeaveRequest(id);
        LeaveLifecycle.assertTransition(r.getStatus(), LeaveRequestStatus.REJECTED);
        if (body == null || body.getReason() == null || body.getReason().isBlank()) {
            throw new BusinessException("A rejection reason is required");
        }
        r.setStatus(LeaveRequestStatus.REJECTED);
        r.setRejectionReason(body.getReason().trim());
        r.setApprovedBy(currentUser());
        r.setApprovedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(r);

        auditService.log("LEAVE_REJECTED", MODULE, "LeaveRequest", saved.getId(),
                LeaveRequestStatus.PENDING.name(), LeaveRequestStatus.REJECTED.name());
        notifyEmployee(saved.getEmployee(), "Leave Rejected",
                String.format("Your %s request %s was rejected: %s",
                        saved.getLeaveType().getName(), saved.getRequestNumber(), saved.getRejectionReason()));
        return toLeaveRequestResponse(saved);
    }

    @Transactional
    public LeaveRequestResponse cancelLeaveRequest(Long id) {
        LeaveRequest r = requireLeaveRequest(id);
        LeaveLifecycle.assertTransition(r.getStatus(), LeaveRequestStatus.CANCELLED);

        // Only the requesting employee or an HR leave operator may withdraw a pending request.
        boolean hrOperator = hasAuthority(Permissions.HR_LEAVE) || hasAuthority(Permissions.HR_LEAVE_APPROVE);
        if (!hrOperator) {
            User me = currentUser();
            boolean owner = me != null && r.getEmployee().getUser() != null
                    && me.getId().equals(r.getEmployee().getUser().getId());
            if (!owner) {
                throw new BusinessException("Not authorized to cancel this leave request");
            }
        }

        r.setStatus(LeaveRequestStatus.CANCELLED);
        LeaveRequest saved = leaveRequestRepository.save(r);
        auditService.log("LEAVE_CANCELLED", MODULE, "LeaveRequest", saved.getId(),
                LeaveRequestStatus.PENDING.name(), LeaveRequestStatus.CANCELLED.name());
        return toLeaveRequestResponse(saved);
    }

    /** Inclusive calendar days. Working-day calendars are a future refinement. */
    BigDecimal calculateLeaveDays(LocalDate start, LocalDate end) {
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        return BigDecimal.valueOf(days).setScale(2, RoundingMode.HALF_UP);
    }

    // ─── Leave balances ──────────────────────────────────────────

    @Transactional
    public List<LeaveBalanceResponse> getLeaveBalances(Long employeeId, Integer year) {
        EmployeeProfile employee = requireEmployee(employeeId);
        int targetYear = year != null ? year : LocalDate.now().getYear();
        List<LeaveBalanceResponse> out = new ArrayList<>();
        for (LeaveType type : leaveTypeRepository.findByActiveTrueOrderByNameAsc()) {
            if (!type.isBalanceControlled()) {
                continue;
            }
            out.add(toLeaveBalanceResponse(getOrCreateBalance(employee, type, targetYear)));
        }
        return out;
    }

    private LeaveBalance getOrCreateBalance(EmployeeProfile employee, LeaveType type, int year) {
        return leaveBalanceRepository
                .findByEmployeeIdAndLeaveTypeIdAndYear(employee.getId(), type.getId(), year)
                .orElseGet(() -> {
                    LeaveBalance b = new LeaveBalance();
                    b.setEmployee(employee);
                    b.setLeaveType(type);
                    b.setYear(year);
                    b.setEntitlement(nvl(type.getDefaultAnnualEntitlement()));
                    b.setUsedDays(ZERO);
                    b.recalculateRemaining();
                    return leaveBalanceRepository.save(b);
                });
    }

    // ─── Deduction types ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DeductionTypeResponse> listDeductionTypes(boolean activeOnly) {
        List<PayrollDeductionType> list = activeOnly
                ? deductionTypeRepository.findByActiveTrueOrderByCodeAsc()
                : deductionTypeRepository.findAllByOrderByCodeAsc();
        return list.stream().map(this::toDeductionTypeResponse).toList();
    }

    @Transactional
    public DeductionTypeResponse createDeductionType(DeductionTypeRequest req) {
        String code = normalizeCode(req.getCode(), req.getName());
        if (deductionTypeRepository.existsByCode(code)) {
            throw new BusinessException("Deduction type code already exists: " + code);
        }
        PayrollDeductionType d = new PayrollDeductionType();
        d.setCode(code);
        applyDeductionTypeFields(d, req);
        PayrollDeductionType saved = deductionTypeRepository.save(d);
        auditService.log("HR_DEDUCTION_TYPE_CREATED", MODULE, "PayrollDeductionType",
                saved.getId(), null, saved.getCode());
        return toDeductionTypeResponse(saved);
    }

    @Transactional
    public DeductionTypeResponse updateDeductionType(Long id, DeductionTypeRequest req) {
        PayrollDeductionType d = requireDeductionType(id);
        String previous = describeDeductionRate(d);
        applyDeductionTypeFields(d, req);
        PayrollDeductionType saved = deductionTypeRepository.save(d);
        auditService.log("HR_DEDUCTION_TYPE_UPDATED", MODULE, "PayrollDeductionType",
                saved.getId(), previous, describeDeductionRate(saved));
        return toDeductionTypeResponse(saved);
    }

    private void applyDeductionTypeFields(PayrollDeductionType d, DeductionTypeRequest req) {
        if (req.getName() != null && !req.getName().isBlank()) {
            d.setName(req.getName().trim());
        }
        if (req.getCategory() != null) d.setCategory(req.getCategory());
        if (req.getRatePercent() != null) {
            if (req.getRatePercent().compareTo(BigDecimal.ZERO) < 0
                    || req.getRatePercent().compareTo(HUNDRED) > 0) {
                throw new BusinessException("Deduction rate must be between 0 and 100 percent");
            }
            d.setRatePercent(req.getRatePercent());
        }
        if (req.getFixedAmount() != null) {
            if (req.getFixedAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("Fixed deduction amount cannot be negative");
            }
            d.setFixedAmount(scale(req.getFixedAmount()));
        }
        if (req.getActive() != null) d.setActive(req.getActive());
        if (req.getApproved() != null) d.setApproved(req.getApproved());
        if (req.getNotes() != null) d.setNotes(req.getNotes());
    }

    private void assertPayrollConfigurationApproved() {
        List<PayrollDeductionType> activeDeductions = deductionTypeRepository.findByActiveTrueOrderByCodeAsc();
        for (PayrollDeductionType d : activeDeductions) {
            if (!d.isApproved()) {
                throw new BusinessException(String.format(
                        "Payroll finalization/payment rejected: deduction type '%s' (%s) is not approved. "
                                + "Official approved statutory rates are required.",
                        d.getCode(), d.getName()));
            }
            if (d.getNotes() != null && d.getNotes().toUpperCase().contains("PLACEHOLDER")) {
                throw new BusinessException(String.format(
                        "Payroll finalization/payment rejected: deduction type '%s' contains unverified placeholder rate notes. "
                                + "Approved statutory configuration is required.",
                        d.getCode()));
            }
        }
    }

    private String describeDeductionRate(PayrollDeductionType d) {
        return d.getRatePercent() != null
                ? d.getRatePercent().toPlainString() + "%"
                : String.valueOf(d.getFixedAmount());
    }

    // ─── Payroll periods ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PayrollPeriodResponse> listPayrollPeriods(PayrollPeriodStatus status) {
        List<PayrollPeriod> list = status != null
                ? payrollPeriodRepository.findByStatusOrderByStartDateDesc(status)
                : payrollPeriodRepository.findAllByOrderByStartDateDesc();
        return list.stream().map(this::toPayrollPeriodResponse).toList();
    }

    @Transactional(readOnly = true)
    public PayrollPeriodResponse getPayrollPeriod(Long id) {
        return toPayrollPeriodResponse(requirePayrollPeriod(id));
    }

    @Transactional
    public PayrollPeriodResponse createPayrollPeriod(PayrollPeriodRequest req) {
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BusinessException("Payroll period end date cannot precede the start date");
        }
        List<PayrollPeriod> overlapping = payrollPeriodRepository
                .findOverlapping(req.getStartDate(), req.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new BusinessException("Payroll period overlaps existing period "
                    + overlapping.get(0).getPeriodCode());
        }
        String code = req.getPeriodCode() != null && !req.getPeriodCode().isBlank()
                ? req.getPeriodCode().trim().toUpperCase()
                : nextPayrollPeriodCode();
        if (payrollPeriodRepository.existsByPeriodCode(code)) {
            throw new BusinessException("Payroll period code already exists: " + code);
        }

        PayrollPeriod p = new PayrollPeriod();
        p.setPeriodCode(code);
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setStatus(PayrollPeriodStatus.OPEN);
        p.setNotes(req.getNotes());
        p.setCreatedBy(currentUser());
        PayrollPeriod saved = payrollPeriodRepository.save(p);
        auditService.log("PAYROLL_PERIOD_CREATED", MODULE, "PayrollPeriod",
                saved.getId(), null, saved.getPeriodCode());
        return toPayrollPeriodResponse(saved);
    }

    /**
     * Builds (or rebuilds) the payroll run for a period. Only OPEN or CALCULATED
     * periods may be calculated — once approved the amounts are frozen.
     */
    @Transactional
    public PayrollRunResponse calculatePayroll(Long periodId) {
        PayrollPeriod period = requirePayrollPeriod(periodId);
        if (!PayrollPeriodLifecycle.isCalculable(period.getStatus())) {
            throw new BusinessException("Payroll cannot be calculated for a "
                    + period.getStatus() + " period");
        }

        int runNumber = payrollRunRepository.maxRunNumber(period.getId()) + 1;
        discardUnapprovedRuns(period);

        List<PayrollDeductionType> deductionTypes = deductionTypeRepository.findByActiveTrueOrderByCodeAsc();
        List<EmployeeProfile> employees = employeeRepository.findPayrollEligible(
                List.of(EmploymentStatus.ACTIVE, EmploymentStatus.ON_LEAVE));

        PayrollRun run = new PayrollRun();
        run.setPayrollPeriod(period);
        run.setRunNumber(runNumber);
        run.setStatus(PayrollRunStatus.CALCULATED);
        run.setCreatedBy(currentUser());
        PayrollRun savedRun = payrollRunRepository.save(run);

        BigDecimal totalGross = ZERO;
        BigDecimal totalDeductions = ZERO;
        BigDecimal totalNet = ZERO;

        for (EmployeeProfile employee : employees) {
            PayrollItem item = buildPayrollItem(employee, savedRun, period, deductionTypes);
            PayrollItem savedItem = payrollItemRepository.save(item);
            payslipRepository.save(buildPayslip(savedItem, savedRun, period));

            totalGross = totalGross.add(savedItem.getGrossSalary());
            totalDeductions = totalDeductions.add(savedItem.getTotalDeductions());
            totalNet = totalNet.add(savedItem.getNetSalary());
        }

        savedRun.setEmployeeCount(employees.size());
        savedRun.setGrossAmount(scale(totalGross));
        savedRun.setDeductionAmount(scale(totalDeductions));
        savedRun.setNetAmount(scale(totalNet));
        savedRun = payrollRunRepository.save(savedRun);

        period.setStatus(PayrollPeriodStatus.CALCULATED);
        period.setCalculatedAt(LocalDateTime.now());
        period.setCalculatedBy(currentUser());
        payrollPeriodRepository.save(period);

        auditService.log("PAYROLL_CALCULATED", MODULE, "PayrollRun", savedRun.getId(),
                null, String.format("%s run %d: %d employees, gross %s, net %s",
                        period.getPeriodCode(), savedRun.getRunNumber(), employees.size(),
                        savedRun.getGrossAmount().toPlainString(), savedRun.getNetAmount().toPlainString()));
        return toPayrollRunResponse(savedRun);
    }

    /** Drops pre-approval runs so a recalculation cannot leave stale items or payslips behind. */
    private void discardUnapprovedRuns(PayrollPeriod period) {
        for (PayrollRun existing : payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(period.getId())) {
            if (existing.getStatus() != PayrollRunStatus.DRAFT
                    && existing.getStatus() != PayrollRunStatus.CALCULATED) {
                throw new BusinessException("Payroll run " + existing.getRunNumber()
                        + " is already " + existing.getStatus() + " and cannot be recalculated");
            }
            payslipRepository.deleteByRunId(existing.getId());
            payrollItemRepository.deleteByPayrollRunId(existing.getId());
            payrollRunRepository.delete(existing);
        }
        payrollRunRepository.flush();
    }

    private PayrollItem buildPayrollItem(EmployeeProfile employee, PayrollRun run, PayrollPeriod period,
                                         List<PayrollDeductionType> deductionTypes) {
        BigDecimal basic = scale(salaryEffectiveOn(employee, period.getEndDate()));
        // V1 pays base salary only; allowance, overtime and bonus inputs land here later.
        BigDecimal allowances = ZERO;
        BigDecimal overtime = ZERO;
        BigDecimal bonuses = ZERO;
        BigDecimal gross = scale(basic.add(allowances).add(overtime).add(bonuses));

        BigDecimal tax = ZERO;
        BigDecimal pension = ZERO;
        BigDecimal other = ZERO;
        if (gross.compareTo(BigDecimal.ZERO) > 0) {
            for (PayrollDeductionType type : deductionTypes) {
                BigDecimal amount = deductionAmount(type, gross);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                switch (type.getCategory()) {
                    case INCOME_TAX -> tax = tax.add(amount);
                    case PENSION -> pension = pension.add(amount);
                    default -> other = other.add(amount);
                }
            }
        }
        BigDecimal deductions = scale(tax.add(pension).add(other));
        if (deductions.compareTo(gross) > 0) {
            throw new BusinessException(String.format(
                    "Configured deductions (%s) exceed gross pay (%s) for %s. Review the deduction types.",
                    deductions.toPlainString(), gross.toPlainString(), employee.getEmployeeNumber()));
        }

        PayrollItem item = new PayrollItem();
        item.setPayrollRun(run);
        item.setEmployee(employee);
        item.setBasicSalary(basic);
        item.setAllowances(allowances);
        item.setOvertime(overtime);
        item.setBonuses(bonuses);
        item.setGrossSalary(gross);
        item.setTax(scale(tax));
        item.setPension(scale(pension));
        item.setOtherDeductions(scale(other));
        item.setTotalDeductions(deductions);
        item.setNetSalary(scale(gross.subtract(deductions)));
        item.setCurrency(employee.getCurrency() != null ? employee.getCurrency() : "ETB");
        item.setStatus(PayrollItemStatus.CALCULATED);
        item.setPaidAmount(ZERO);
        item.setPaymentStatus(PayrollPaymentStatus.PENDING);
        return item;
    }

    private BigDecimal deductionAmount(PayrollDeductionType type, BigDecimal gross) {
        if (type.getRatePercent() != null && type.getRatePercent().compareTo(BigDecimal.ZERO) > 0) {
            return gross.multiply(type.getRatePercent())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }
        return type.getFixedAmount() != null ? scale(type.getFixedAmount()) : ZERO;
    }

    private BigDecimal salaryEffectiveOn(EmployeeProfile employee, LocalDate onDate) {
        List<EmployeeSalaryHistory> history = salaryHistoryRepository.findEffectiveOn(employee.getId(), onDate);
        if (!history.isEmpty()) {
            return nvl(history.get(0).getBasicSalary());
        }
        return nvl(employee.getBasicSalary());
    }

    private Payslip buildPayslip(PayrollItem item, PayrollRun run, PayrollPeriod period) {
        Payslip p = new Payslip();
        p.setPayslipNumber(nextPayslipNumber());
        p.setPayrollItem(item);
        p.setEmployee(item.getEmployee());
        p.setPeriod(period);
        p.setRun(run);
        p.setPeriodStartDate(period.getStartDate());
        p.setPeriodEndDate(period.getEndDate());
        p.setBasicSalary(item.getBasicSalary());
        p.setAllowances(item.getAllowances());
        p.setOvertime(item.getOvertime());
        p.setBonuses(item.getBonuses());
        p.setGrossSalary(item.getGrossSalary());
        p.setTax(item.getTax());
        p.setPension(item.getPension());
        p.setOtherDeductions(item.getOtherDeductions());
        p.setTotalDeductions(item.getTotalDeductions());
        p.setNetSalary(item.getNetSalary());
        p.setCurrency(item.getCurrency());
        p.setPaymentStatus(PayrollPaymentStatus.PENDING);
        return p;
    }

    @Transactional
    public PayrollPeriodResponse approvePayroll(Long periodId) {
        assertHasAuthority(Permissions.HR_PAYROLL_APPROVE);
        assertPayrollConfigurationApproved();
        PayrollPeriod period = requirePayrollPeriod(periodId);
        if (period.getStatus() == PayrollPeriodStatus.APPROVED) {
            return toPayrollPeriodResponse(period);
        }
        PayrollPeriodLifecycle.assertTransition(period.getStatus(), PayrollPeriodStatus.APPROVED);

        PayrollRun run = requireCurrentRun(period);
        User approver = currentUser();
        if (approver != null) {
            boolean approvingOwnPay = payrollItemRepository.findByPayrollRunIdOrderByIdAsc(run.getId()).stream()
                    .anyMatch(i -> i.getEmployee().getUser() != null
                            && approver.getId().equals(i.getEmployee().getUser().getId()));
            if (approvingOwnPay) {
                throw new BusinessException("You cannot approve a payroll run that includes your own pay");
            }
        }

        run.setStatus(PayrollRunStatus.APPROVED);
        run.setApprovedBy(approver);
        run.setApprovedAt(LocalDateTime.now());
        payrollRunRepository.save(run);
        payrollItemRepository.findByPayrollRunIdOrderByIdAsc(run.getId()).forEach(i -> {
            i.setStatus(PayrollItemStatus.APPROVED);
            payrollItemRepository.save(i);
        });

        period.setStatus(PayrollPeriodStatus.APPROVED);
        period.setApprovedBy(approver);
        period.setApprovedAt(LocalDateTime.now());
        PayrollPeriod saved = payrollPeriodRepository.save(period);

        auditService.log("PAYROLL_APPROVED", MODULE, "PayrollPeriod", saved.getId(),
                PayrollPeriodStatus.CALCULATED.name(), PayrollPeriodStatus.APPROVED.name());
        notificationService.createRoleNotification("Payroll Approved",
                String.format("Payroll %s approved: net %s across %d employee(s)",
                        saved.getPeriodCode(), run.getNetAmount().toPlainString(), run.getEmployeeCount()),
                Role.FINANCE);
        return toPayrollPeriodResponse(saved);
    }

    /**
     * Posts the approved run to Finance. Accrues the gross wage bill as an expense
     * against salary payable, then reclasses withheld deductions into their own
     * liability. Repeat calls are a no-op — both the period status and the Finance
     * {@code (module, referenceType, referenceId)} key guard against double posting.
     */
    @Transactional
    public PayrollPeriodResponse processPayroll(Long periodId) {
        assertHasAuthority(Permissions.HR_PAYROLL_PROCESS);
        assertPayrollConfigurationApproved();
        PayrollPeriod period = requirePayrollPeriod(periodId);
        if (period.getStatus() == PayrollPeriodStatus.PROCESSED
                || period.getStatus() == PayrollPeriodStatus.CLOSED) {
            return toPayrollPeriodResponse(period);
        }
        PayrollPeriodLifecycle.assertTransition(period.getStatus(), PayrollPeriodStatus.PROCESSED);

        PayrollRun run = requireCurrentRun(period);
        if (run.getStatus() != PayrollRunStatus.APPROVED) {
            throw new BusinessException("Payroll run must be APPROVED before processing");
        }

        String description = "Payroll " + period.getPeriodCode();
        if (nvl(run.getGrossAmount()).compareTo(BigDecimal.ZERO) > 0) {
            financeService.postTransaction(TransactionType.EXPENSE, run.getGrossAmount(), BusinessUnit.GENERAL,
                    AccountCode.EXP_SALARIES.getCode(), AccountCode.LIABILITY_SALARY_PAYABLE.getCode(),
                    description + " — gross salary expense", SOURCE_MODULE,
                    FinanceSourceTypes.REF_HR_PAYROLL_EXPENSE, run.getId());
        }
        if (nvl(run.getDeductionAmount()).compareTo(BigDecimal.ZERO) > 0) {
            financeService.postTransaction(TransactionType.TRANSFER, run.getDeductionAmount(), BusinessUnit.GENERAL,
                    AccountCode.LIABILITY_SALARY_PAYABLE.getCode(),
                    AccountCode.LIABILITY_PAYROLL_DEDUCTIONS.getCode(),
                    description + " — withheld deductions", SOURCE_MODULE,
                    FinanceSourceTypes.REF_HR_PAYROLL_DEDUCTION, run.getId());
        }

        User processor = currentUser();
        run.setStatus(PayrollRunStatus.PROCESSED);
        run.setProcessedBy(processor);
        run.setProcessedAt(LocalDateTime.now());
        payrollRunRepository.save(run);

        List<PayrollItem> items = payrollItemRepository.findByPayrollRunIdOrderByIdAsc(run.getId());
        for (PayrollItem item : items) {
            item.setStatus(PayrollItemStatus.PROCESSED);
            payrollItemRepository.save(item);
            notifyEmployee(item.getEmployee(), "Payslip Available",
                    String.format("Your payslip for %s is available: net %s %s",
                            period.getPeriodCode(), item.getNetSalary().toPlainString(), item.getCurrency()));
        }

        period.setStatus(PayrollPeriodStatus.PROCESSED);
        period.setProcessedBy(processor);
        period.setProcessedAt(LocalDateTime.now());
        PayrollPeriod saved = payrollPeriodRepository.save(period);

        auditService.log("PAYROLL_PROCESSED", MODULE, "PayrollPeriod", saved.getId(),
                PayrollPeriodStatus.APPROVED.name(),
                String.format("gross %s, deductions %s, net %s",
                        run.getGrossAmount().toPlainString(), run.getDeductionAmount().toPlainString(),
                        run.getNetAmount().toPlainString()));
        notificationService.createRoleNotification("Payroll Posted to Finance",
                String.format("Payroll %s posted: salary payable %s awaiting disbursement",
                        saved.getPeriodCode(), run.getNetAmount().toPlainString()),
                Role.FINANCE);
        return toPayrollPeriodResponse(saved);
    }

    @Transactional
    public PayrollPeriodResponse closePayroll(Long periodId) {
        assertHasAuthority(Permissions.HR_PAYROLL);
        PayrollPeriod period = requirePayrollPeriod(periodId);
        if (period.getStatus() == PayrollPeriodStatus.CLOSED) {
            return toPayrollPeriodResponse(period);
        }
        PayrollPeriodLifecycle.assertTransition(period.getStatus(), PayrollPeriodStatus.CLOSED);
        period.setStatus(PayrollPeriodStatus.CLOSED);
        period.setClosedAt(LocalDateTime.now());
        PayrollPeriod saved = payrollPeriodRepository.save(period);
        auditService.log("PAYROLL_CLOSED", MODULE, "PayrollPeriod", saved.getId(),
                PayrollPeriodStatus.PROCESSED.name(), PayrollPeriodStatus.CLOSED.name());
        return toPayrollPeriodResponse(saved);
    }

    @Transactional
    public PayrollPeriodResponse cancelPayrollPeriod(Long periodId) {
        assertHasAuthority(Permissions.HR_PAYROLL);
        PayrollPeriod period = requirePayrollPeriod(periodId);
        PayrollPeriodLifecycle.assertTransition(period.getStatus(), PayrollPeriodStatus.CANCELLED);
        PayrollPeriodStatus previous = period.getStatus();

        for (PayrollRun run : payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(period.getId())) {
            run.setStatus(PayrollRunStatus.CANCELLED);
            payrollRunRepository.save(run);
            payrollItemRepository.findByPayrollRunIdOrderByIdAsc(run.getId()).forEach(i -> {
                i.setStatus(PayrollItemStatus.CANCELLED);
                payrollItemRepository.save(i);
            });
        }

        period.setStatus(PayrollPeriodStatus.CANCELLED);
        PayrollPeriod saved = payrollPeriodRepository.save(period);
        auditService.log("PAYROLL_PERIOD_CANCELLED", MODULE, "PayrollPeriod", saved.getId(),
                previous.name(), PayrollPeriodStatus.CANCELLED.name());
        return toPayrollPeriodResponse(saved);
    }

    // ─── Runs, items, payslips ───────────────────────────────────

    @Transactional(readOnly = true)
    public List<PayrollRunResponse> listPayrollRuns(Long periodId) {
        List<PayrollRun> runs = periodId != null
                ? payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(periodId)
                : payrollRunRepository.findAll();
        return runs.stream().map(this::toPayrollRunResponse).toList();
    }

    @Transactional(readOnly = true)
    public PayrollRunResponse getPayrollRun(Long id) {
        return toPayrollRunResponse(requirePayrollRun(id));
    }

    @Transactional(readOnly = true)
    public List<PayrollItemResponse> listPayrollItems(Long runId) {
        requirePayrollRun(runId);
        return payrollItemRepository.findByPayrollRunIdOrderByIdAsc(runId)
                .stream().map(this::toPayrollItemResponse).toList();
    }

    @Transactional(readOnly = true)
    public PayslipResponse getPayslip(Long id) {
        Payslip p = requirePayslip(id);
        if (!canViewPayslip(p)) {
            throw new BusinessException("Not authorized to view this payslip");
        }
        return toPayslipResponse(p);
    }

    private boolean canViewPayslip(Payslip p) {
        // Directory compensation stays gated by canSeeCompensation(); payslip detail is
        // available to HR readers (ops oversight), payroll operators, and the owner.
        if (canSeeCompensation() || hasAuthority(Permissions.HR_READ)) {
            return true;
        }
        User me = currentUser();
        return me != null && p.getEmployee().getUser() != null
                && me.getId().equals(p.getEmployee().getUser().getId());
    }

    // ─── Payroll payments ────────────────────────────────────────

    /**
     * Settles salary payable in cash or bank. Never touches the salary expense
     * account — the cost was already recognised when the run was processed.
     */
    @Transactional
    public PayrollPaymentResponse payPayroll(Long runId, PayrollPaymentRequest req) {
        assertHasAuthority(Permissions.HR_PAYROLL_PROCESS);
        assertPayrollConfigurationApproved();
        PayrollRun run = requirePayrollRun(runId);
        PayrollPeriod period = run.getPayrollPeriod();
        if (!PayrollPeriodLifecycle.isPayable(period.getStatus())) {
            throw new BusinessException("Payroll must be PROCESSED before it can be paid");
        }

        PaymentMethod method = req.getPaymentMethod() != null ? req.getPaymentMethod() : PaymentMethod.BANK_TRANSFER;
        String settlementAccount = settlementAccountFor(method);
        LocalDate paymentDate = req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now();

        List<PayrollItem> targets;
        if (req.getPayrollItemId() != null) {
            PayrollItem item = requirePayrollItem(req.getPayrollItemId());
            if (!item.getPayrollRun().getId().equals(run.getId())) {
                throw new BusinessException("Payroll item does not belong to run " + run.getId());
            }
            targets = List.of(item);
        } else {
            targets = payrollItemRepository.findByPayrollRunIdOrderByIdAsc(run.getId()).stream()
                    .filter(i -> outstandingOf(i).compareTo(BigDecimal.ZERO) > 0)
                    .toList();
        }
        if (targets.isEmpty()) {
            throw new BusinessException("Nothing outstanding to pay on this payroll run");
        }

        BigDecimal outstanding = targets.stream()
                .map(this::outstandingOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Nothing outstanding to pay on this payroll run");
        }

        BigDecimal amount = req.getAmount() != null ? scale(req.getAmount()) : outstanding;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be greater than zero");
        }
        if (amount.compareTo(outstanding) > 0) {
            throw new BusinessException("Payment exceeds the outstanding net of " + outstanding.toPlainString());
        }
        if (req.getPayrollItemId() == null && amount.compareTo(outstanding) != 0) {
            throw new BusinessException("A batch payroll payment must settle the full outstanding net of "
                    + outstanding.toPlainString());
        }

        PayrollPayment payment = new PayrollPayment();
        payment.setPaymentNumber(nextPayrollPaymentNumber());
        payment.setPayrollRun(run);
        payment.setPayrollItem(req.getPayrollItemId() != null ? targets.get(0) : null);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setPaymentDate(paymentDate);
        payment.setNotes(req.getNotes());
        payment.setRecordedBy(currentUser());
        PayrollPayment savedPayment = payrollPaymentRepository.saveAndFlush(payment);

        financeService.postTransaction(TransactionType.TRANSFER, amount, BusinessUnit.GENERAL,
                AccountCode.LIABILITY_SALARY_PAYABLE.getCode(), settlementAccount,
                "Payroll disbursement " + period.getPeriodCode() + " (" + savedPayment.getPaymentNumber() + ")",
                SOURCE_MODULE, FinanceSourceTypes.REF_HR_PAYROLL_PAYMENT, savedPayment.getId(),
                method, paymentDate);

        BigDecimal remaining = amount;
        for (PayrollItem item : targets) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal due = outstandingOf(item);
            BigDecimal applied = due.min(remaining);
            item.setPaidAmount(scale(nvl(item.getPaidAmount()).add(applied)));
            boolean settled = outstandingOf(item).compareTo(BigDecimal.ZERO) <= 0;
            item.setPaymentStatus(settled ? PayrollPaymentStatus.PAID
                    : nvl(item.getPaidAmount()).compareTo(BigDecimal.ZERO) > 0
                        ? PayrollPaymentStatus.PARTIAL : PayrollPaymentStatus.PENDING);
            if (settled) {
                item.setStatus(PayrollItemStatus.PAID);
            }
            payrollItemRepository.save(item);
            payslipRepository.findByPayrollItemId(item.getId()).ifPresent(slip -> {
                slip.setPaymentStatus(item.getPaymentStatus());
                payslipRepository.save(slip);
            });
            remaining = remaining.subtract(applied);
        }

        auditService.log("PAYROLL_PAYMENT_POSTED", MODULE, "PayrollPayment", savedPayment.getId(),
                null, String.format("%s %s via %s", savedPayment.getPaymentNumber(),
                        amount.toPlainString(), method));
        notificationService.createRoleNotification("Payroll Disbursed",
                String.format("Payroll %s: %s disbursed via %s",
                        period.getPeriodCode(), amount.toPlainString(), method),
                Role.FINANCE);
        return toPayrollPaymentResponse(savedPayment);
    }

    @Transactional(readOnly = true)
    public List<PayrollPaymentResponse> listPayrollPayments(Long runId) {
        requirePayrollRun(runId);
        return payrollPaymentRepository.findByPayrollRunIdOrderByIdDesc(runId)
                .stream().map(this::toPayrollPaymentResponse).toList();
    }

    private String settlementAccountFor(PaymentMethod method) {
        if (method == PaymentMethod.CREDIT) {
            throw new BusinessException("Payroll cannot be disbursed on credit");
        }
        return method == PaymentMethod.CASH
                ? AccountCode.ASSET_CASH.getCode()
                : AccountCode.ASSET_BANK.getCode();
    }

    private BigDecimal outstandingOf(PayrollItem item) {
        return scale(nvl(item.getNetSalary()).subtract(nvl(item.getPaidAmount())));
    }

    // ─── Dashboard & reports ─────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        Map<String, Object> dash = new LinkedHashMap<>();
        dash.put("totalEmployees", employeeRepository.count());
        dash.put("activeHeadcount", employeeRepository.countByEmploymentStatus(EmploymentStatus.ACTIVE));
        dash.put("onLeave", employeeRepository.countByEmploymentStatus(EmploymentStatus.ON_LEAVE));

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (EmploymentStatus s : EmploymentStatus.values()) {
            byStatus.put(s.name(), employeeRepository.countByEmploymentStatus(s));
        }
        dash.put("employeesByStatus", byStatus);
        dash.put("headcountByDepartment", headcountByDepartment());

        LocalDate today = LocalDate.now();
        Map<String, Long> attendanceToday = new LinkedHashMap<>();
        attendanceToday.put("recorded", attendanceRepository.countByAttendanceDate(today));
        for (AttendanceStatus s : AttendanceStatus.values()) {
            attendanceToday.put(s.name(), attendanceRepository.countByAttendanceDateAndStatus(today, s));
        }
        dash.put("attendanceToday", attendanceToday);

        dash.put("pendingLeaveRequests", leaveRequestRepository.countByStatus(LeaveRequestStatus.PENDING));
        dash.put("approvedLeaveRequests", leaveRequestRepository.countByStatus(LeaveRequestStatus.APPROVED));

        currentPayrollPeriod().ifPresent(p -> {
            dash.put("currentPayrollPeriod", p.getPeriodCode());
            dash.put("currentPayrollStatus", p.getStatus());
            currentRun(p).ifPresent(run -> {
                dash.put("currentPayrollGross", nvl(run.getGrossAmount()));
                dash.put("currentPayrollNet", nvl(run.getNetAmount()));
                dash.put("currentPayrollEmployees", run.getEmployeeCount());
            });
        });
        dash.put("salaryPayableOutstanding", nvl(payrollItemRepository.sumOutstandingNet()));
        return dash;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reports() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("headcountByDepartment", headcountByDepartment());

        Map<String, Long> byType = new LinkedHashMap<>();
        for (EmploymentType t : EmploymentType.values()) {
            byType.put(t.name(), employeeRepository.findAll().stream()
                    .filter(e -> e.getEmploymentType() == t).count());
        }
        report.put("employeesByEmploymentType", byType);

        Map<String, Long> leaveByStatus = new LinkedHashMap<>();
        for (LeaveRequestStatus s : LeaveRequestStatus.values()) {
            leaveByStatus.put(s.name(), leaveRequestRepository.countByStatus(s));
        }
        report.put("leaveRequestsByStatus", leaveByStatus);

        List<Map<String, Object>> payrollByPeriod = new ArrayList<>();
        for (PayrollPeriod period : payrollPeriodRepository.findAllByOrderByStartDateDesc()) {
            Optional<PayrollRun> run = currentRun(period);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("periodCode", period.getPeriodCode());
            row.put("startDate", period.getStartDate());
            row.put("endDate", period.getEndDate());
            row.put("status", period.getStatus());
            row.put("employeeCount", run.map(PayrollRun::getEmployeeCount).orElse(0));
            row.put("gross", run.map(r -> nvl(r.getGrossAmount())).orElse(ZERO));
            row.put("deductions", run.map(r -> nvl(r.getDeductionAmount())).orElse(ZERO));
            row.put("net", run.map(r -> nvl(r.getNetAmount())).orElse(ZERO));
            payrollByPeriod.add(row);
        }
        report.put("payrollByPeriod", payrollByPeriod);
        report.put("payrollCostFromFinance", payrollCostFromFinance());
        report.put("salaryPayableOutstanding", nvl(payrollItemRepository.sumOutstandingNet()));
        report.put("note", "Payroll cost is taken from Finance EXPENSE postings with sourceModule=HR. "
                + "Deduction rates are configuration and must be maintained per the regulations that apply.");
        return report;
    }

    private Map<String, Long> headcountByDepartment() {
        Map<String, Long> byDepartment = new LinkedHashMap<>();
        for (Object[] row : employeeRepository.countGroupedByDepartment()) {
            byDepartment.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return byDepartment;
    }

    private BigDecimal payrollCostFromFinance() {
        return financeService.getAllTransactions().stream()
                .filter(t -> SOURCE_MODULE.equals(t.getSourceModule()))
                .filter(t -> t.getTransactionType() == TransactionType.EXPENSE)
                .map(FinanceTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<PayrollPeriod> currentPayrollPeriod() {
        return payrollPeriodRepository.findAllByOrderByStartDateDesc().stream()
                .filter(p -> p.getStatus() != PayrollPeriodStatus.CANCELLED)
                .findFirst();
    }

    private Optional<PayrollRun> currentRun(PayrollPeriod period) {
        return payrollRunRepository.findByPayrollPeriodIdOrderByRunNumberDesc(period.getId()).stream()
                .filter(r -> r.getStatus() != PayrollRunStatus.CANCELLED)
                .findFirst();
    }

    private PayrollRun requireCurrentRun(PayrollPeriod period) {
        return currentRun(period).orElseThrow(() ->
                new BusinessException("No payroll run exists for period " + period.getPeriodCode()
                        + ". Calculate payroll first."));
    }

    // ─── Self-service ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public EmployeeResponse myProfile() {
        // Compensation is always visible on one's own record.
        return toEmployeeResponse(requireOwnEmployeeProfile(), true);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> myAttendance(LocalDate from, LocalDate to) {
        EmployeeProfile me = requireOwnEmployeeProfile();
        List<AttendanceRecord> list = (from != null && to != null)
                ? attendanceRepository.findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
                        me.getId(), from, to)
                : attendanceRepository.findByEmployeeIdOrderByAttendanceDateDesc(me.getId());
        return list.stream().map(this::toAttendanceResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> myLeaveRequests() {
        EmployeeProfile me = requireOwnEmployeeProfile();
        return leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(me.getId())
                .stream().map(this::toLeaveRequestResponse).toList();
    }

    @Transactional
    public List<LeaveBalanceResponse> myLeaveBalances(Integer year) {
        EmployeeProfile me = requireOwnEmployeeProfile();
        return getLeaveBalances(me.getId(), year);
    }

    @Transactional(readOnly = true)
    public List<PayslipResponse> myPayslips() {
        EmployeeProfile me = requireOwnEmployeeProfile();
        return payslipRepository.findByEmployeeIdOrderByIdDesc(me.getId())
                .stream().map(this::toPayslipResponse).toList();
    }

    /** Resolves the caller's own employee record. A client-supplied id is never trusted here. */
    private EmployeeProfile requireOwnEmployeeProfile() {
        User me = currentUser();
        if (me == null) {
            throw new BusinessException("No authenticated user");
        }
        return employeeRepository.findByUserId(me.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No employee record is linked to your account"));
    }

    private void notifyEmployee(EmployeeProfile employee, String title, String message) {
        if (employee.getUser() != null) {
            notificationService.createUserNotification(title, message, employee.getUser());
        }
    }

    // ─── Mappers ─────────────────────────────────────────────────

    private DepartmentResponse toDepartmentResponse(HrDepartment d) {
        return DepartmentResponse.builder()
                .id(d.getId())
                .code(d.getCode())
                .name(d.getName())
                .description(d.getDescription())
                .active(d.isActive())
                .employeeCount(employeeRepository.countByDepartmentId(d.getId()))
                .createdAt(d.getCreatedAt())
                .build();
    }

    private PositionResponse toPositionResponse(HrPosition p) {
        return PositionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .active(p.isActive())
                .employeeCount(employeeRepository.countByPositionId(p.getId()))
                .createdAt(p.getCreatedAt())
                .build();
    }

    private EmployeeResponse toEmployeeResponse(EmployeeProfile e, boolean includeCompensation) {
        return EmployeeResponse.builder()
                .id(e.getId())
                .employeeNumber(e.getEmployeeNumber())
                .firstName(e.getFirstName())
                .middleName(e.getMiddleName())
                .lastName(e.getLastName())
                .fullName(e.getFullName())
                .phone(e.getPhone())
                .email(e.getEmail())
                .address(e.getAddress())
                .hireDate(e.getHireDate())
                .employmentType(e.getEmploymentType())
                .employmentStatus(e.getEmploymentStatus())
                .departmentId(e.getDepartment() != null ? e.getDepartment().getId() : null)
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .positionId(e.getPosition() != null ? e.getPosition().getId() : null)
                .positionName(e.getPosition() != null ? e.getPosition().getName() : null)
                .managerId(e.getManager() != null ? e.getManager().getId() : null)
                .managerName(e.getManager() != null ? e.getManager().getFullName() : null)
                .basicSalary(includeCompensation ? nvl(e.getBasicSalary()) : null)
                .currency(e.getCurrency())
                .bankName(includeCompensation ? e.getBankName() : null)
                .bankAccountRef(includeCompensation ? e.getBankAccountRef() : null)
                .emergencyContact(e.getEmergencyContact())
                .notes(e.getNotes())
                .terminationDate(e.getTerminationDate())
                .terminationReason(e.getTerminationReason())
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .erpAccessEnabled(e.getUser() != null)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private SalaryHistoryResponse toSalaryHistoryResponse(EmployeeSalaryHistory h) {
        return SalaryHistoryResponse.builder()
                .id(h.getId())
                .employeeId(h.getEmployee().getId())
                .employeeNumber(h.getEmployee().getEmployeeNumber())
                .effectiveDate(h.getEffectiveDate())
                .basicSalary(h.getBasicSalary())
                .currency(h.getCurrency())
                .reason(h.getReason())
                .approvedByName(h.getApprovedBy() != null ? h.getApprovedBy().getFullName() : null)
                .createdAt(h.getCreatedAt())
                .build();
    }

    private AttendanceResponse toAttendanceResponse(AttendanceRecord a) {
        EmployeeProfile e = a.getEmployee();
        return AttendanceResponse.builder()
                .id(a.getId())
                .employeeId(e.getId())
                .employeeNumber(e.getEmployeeNumber())
                .employeeName(e.getFullName())
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .attendanceDate(a.getAttendanceDate())
                .checkIn(a.getCheckIn())
                .checkOut(a.getCheckOut())
                .status(a.getStatus())
                .hoursWorked(nvl(a.getHoursWorked()))
                .notes(a.getNotes())
                .recordedByName(a.getRecordedBy() != null ? a.getRecordedBy().getFullName() : null)
                .createdAt(a.getCreatedAt())
                .build();
    }

    private LeaveTypeResponse toLeaveTypeResponse(LeaveType t) {
        return LeaveTypeResponse.builder()
                .id(t.getId())
                .code(t.getCode())
                .name(t.getName())
                .description(t.getDescription())
                .balanceControlled(t.isBalanceControlled())
                .defaultAnnualEntitlement(nvl(t.getDefaultAnnualEntitlement()))
                .paid(t.isPaid())
                .active(t.isActive())
                .createdAt(t.getCreatedAt())
                .build();
    }

    private LeaveRequestResponse toLeaveRequestResponse(LeaveRequest r) {
        EmployeeProfile e = r.getEmployee();
        return LeaveRequestResponse.builder()
                .id(r.getId())
                .requestNumber(r.getRequestNumber())
                .employeeId(e.getId())
                .employeeNumber(e.getEmployeeNumber())
                .employeeName(e.getFullName())
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .leaveTypeId(r.getLeaveType().getId())
                .leaveTypeName(r.getLeaveType().getName())
                .paid(r.getLeaveType().isPaid())
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .totalDays(nvl(r.getTotalDays()))
                .reason(r.getReason())
                .status(r.getStatus())
                .approvedByName(r.getApprovedBy() != null ? r.getApprovedBy().getFullName() : null)
                .approvedAt(r.getApprovedAt())
                .rejectionReason(r.getRejectionReason())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private LeaveBalanceResponse toLeaveBalanceResponse(LeaveBalance b) {
        return LeaveBalanceResponse.builder()
                .id(b.getId())
                .employeeId(b.getEmployee().getId())
                .employeeNumber(b.getEmployee().getEmployeeNumber())
                .employeeName(b.getEmployee().getFullName())
                .leaveTypeId(b.getLeaveType().getId())
                .leaveTypeCode(b.getLeaveType().getCode())
                .leaveTypeName(b.getLeaveType().getName())
                .year(b.getYear())
                .entitlement(nvl(b.getEntitlement()))
                .usedDays(nvl(b.getUsedDays()))
                .remainingDays(nvl(b.getRemainingDays()))
                .build();
    }

    private DeductionTypeResponse toDeductionTypeResponse(PayrollDeductionType d) {
        return DeductionTypeResponse.builder()
                .id(d.getId())
                .code(d.getCode())
                .name(d.getName())
                .category(d.getCategory())
                .ratePercent(d.getRatePercent())
                .fixedAmount(d.getFixedAmount())
                .active(d.isActive())
                .approved(d.isApproved())
                .notes(d.getNotes())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private PayrollPeriodResponse toPayrollPeriodResponse(PayrollPeriod p) {
        Optional<PayrollRun> run = currentRun(p);
        BigDecimal net = run.map(r -> nvl(r.getNetAmount())).orElse(ZERO);
        BigDecimal paid = run.map(r -> payrollItemRepository.findByPayrollRunIdOrderByIdAsc(r.getId()).stream()
                        .map(i -> nvl(i.getPaidAmount()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .orElse(ZERO);
        return PayrollPeriodResponse.builder()
                .id(p.getId())
                .periodCode(p.getPeriodCode())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .status(p.getStatus())
                .notes(p.getNotes())
                .currentRunId(run.map(PayrollRun::getId).orElse(null))
                .employeeCount(run.map(PayrollRun::getEmployeeCount).orElse(0))
                .grossAmount(run.map(r -> nvl(r.getGrossAmount())).orElse(ZERO))
                .deductionAmount(run.map(r -> nvl(r.getDeductionAmount())).orElse(ZERO))
                .netAmount(net)
                .paidAmount(scale(paid))
                .outstandingAmount(scale(net.subtract(paid)))
                .createdByName(p.getCreatedBy() != null ? p.getCreatedBy().getFullName() : null)
                .approvedByName(p.getApprovedBy() != null ? p.getApprovedBy().getFullName() : null)
                .processedByName(p.getProcessedBy() != null ? p.getProcessedBy().getFullName() : null)
                .calculatedAt(p.getCalculatedAt())
                .approvedAt(p.getApprovedAt())
                .processedAt(p.getProcessedAt())
                .closedAt(p.getClosedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private PayrollRunResponse toPayrollRunResponse(PayrollRun r) {
        PayrollPeriod p = r.getPayrollPeriod();
        BigDecimal paid = payrollItemRepository.findByPayrollRunIdOrderByIdAsc(r.getId()).stream()
                .map(i -> nvl(i.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = nvl(r.getNetAmount());
        return PayrollRunResponse.builder()
                .id(r.getId())
                .payrollPeriodId(p.getId())
                .periodCode(p.getPeriodCode())
                .periodStartDate(p.getStartDate())
                .periodEndDate(p.getEndDate())
                .runNumber(r.getRunNumber())
                .status(r.getStatus())
                .employeeCount(r.getEmployeeCount())
                .grossAmount(nvl(r.getGrossAmount()))
                .deductionAmount(nvl(r.getDeductionAmount()))
                .netAmount(net)
                .paidAmount(scale(paid))
                .outstandingAmount(scale(net.subtract(paid)))
                .currency(r.getCurrency())
                .createdByName(r.getCreatedBy() != null ? r.getCreatedBy().getFullName() : null)
                .approvedByName(r.getApprovedBy() != null ? r.getApprovedBy().getFullName() : null)
                .processedByName(r.getProcessedBy() != null ? r.getProcessedBy().getFullName() : null)
                .approvedAt(r.getApprovedAt())
                .processedAt(r.getProcessedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private PayrollItemResponse toPayrollItemResponse(PayrollItem i) {
        EmployeeProfile e = i.getEmployee();
        Optional<Payslip> slip = payslipRepository.findByPayrollItemId(i.getId());
        return PayrollItemResponse.builder()
                .id(i.getId())
                .payrollRunId(i.getPayrollRun().getId())
                .payrollPeriodId(i.getPayrollRun().getPayrollPeriod().getId())
                .periodCode(i.getPayrollRun().getPayrollPeriod().getPeriodCode())
                .employeeId(e.getId())
                .employeeNumber(e.getEmployeeNumber())
                .employeeName(e.getFullName())
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .positionName(e.getPosition() != null ? e.getPosition().getName() : null)
                .basicSalary(nvl(i.getBasicSalary()))
                .allowances(nvl(i.getAllowances()))
                .overtime(nvl(i.getOvertime()))
                .bonuses(nvl(i.getBonuses()))
                .grossSalary(nvl(i.getGrossSalary()))
                .tax(nvl(i.getTax()))
                .pension(nvl(i.getPension()))
                .otherDeductions(nvl(i.getOtherDeductions()))
                .totalDeductions(nvl(i.getTotalDeductions()))
                .netSalary(nvl(i.getNetSalary()))
                .paidAmount(nvl(i.getPaidAmount()))
                .outstandingAmount(outstandingOf(i))
                .currency(i.getCurrency())
                .status(i.getStatus())
                .paymentStatus(i.getPaymentStatus())
                .payslipId(slip.map(Payslip::getId).orElse(null))
                .payslipNumber(slip.map(Payslip::getPayslipNumber).orElse(null))
                .build();
    }

    private PayslipResponse toPayslipResponse(Payslip p) {
        EmployeeProfile e = p.getEmployee();
        return PayslipResponse.builder()
                .id(p.getId())
                .payslipNumber(p.getPayslipNumber())
                .payrollItemId(p.getPayrollItem().getId())
                .payrollRunId(p.getRun().getId())
                .payrollPeriodId(p.getPeriod().getId())
                .periodCode(p.getPeriod().getPeriodCode())
                .periodStartDate(p.getPeriodStartDate())
                .periodEndDate(p.getPeriodEndDate())
                .employeeId(e.getId())
                .employeeNumber(e.getEmployeeNumber())
                .employeeName(e.getFullName())
                .departmentName(e.getDepartment() != null ? e.getDepartment().getName() : null)
                .positionName(e.getPosition() != null ? e.getPosition().getName() : null)
                .basicSalary(nvl(p.getBasicSalary()))
                .allowances(nvl(p.getAllowances()))
                .overtime(nvl(p.getOvertime()))
                .bonuses(nvl(p.getBonuses()))
                .grossSalary(nvl(p.getGrossSalary()))
                .tax(nvl(p.getTax()))
                .pension(nvl(p.getPension()))
                .otherDeductions(nvl(p.getOtherDeductions()))
                .totalDeductions(nvl(p.getTotalDeductions()))
                .netSalary(nvl(p.getNetSalary()))
                .currency(p.getCurrency())
                .paymentStatus(p.getPaymentStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private PayrollPaymentResponse toPayrollPaymentResponse(PayrollPayment p) {
        EmployeeProfile employee = p.getPayrollItem() != null ? p.getPayrollItem().getEmployee() : null;
        return PayrollPaymentResponse.builder()
                .id(p.getId())
                .paymentNumber(p.getPaymentNumber())
                .payrollRunId(p.getPayrollRun().getId())
                .payrollItemId(p.getPayrollItem() != null ? p.getPayrollItem().getId() : null)
                .employeeId(employee != null ? employee.getId() : null)
                .employeeName(employee != null ? employee.getFullName() : null)
                .amount(nvl(p.getAmount()))
                .paymentMethod(p.getPaymentMethod())
                .paymentDate(p.getPaymentDate())
                .notes(p.getNotes())
                .recordedByName(p.getRecordedBy() != null ? p.getRecordedBy().getFullName() : null)
                .createdAt(p.getCreatedAt())
                .build();
    }

    // ─── Require helpers ─────────────────────────────────────────

    private HrDepartment requireDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
    }

    private HrPosition requirePosition(Long id) {
        return positionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));
    }

    private EmployeeProfile requireEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    private AttendanceRecord requireAttendance(Long id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));
    }

    private LeaveType requireLeaveType(Long id) {
        return leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave type not found"));
    }

    private LeaveRequest requireLeaveRequest(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
    }

    private PayrollDeductionType requireDeductionType(Long id) {
        return deductionTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deduction type not found"));
    }

    private PayrollPeriod requirePayrollPeriod(Long id) {
        return payrollPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll period not found"));
    }

    private PayrollRun requirePayrollRun(Long id) {
        return payrollRunRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found"));
    }

    private PayrollItem requirePayrollItem(Long id) {
        return payrollItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll item not found"));
    }

    private Payslip requirePayslip(Long id) {
        return payslipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found"));
    }

    // ─── Number generators ───────────────────────────────────────

    private String nextEmployeeNumber() {
        return nextNumber(SEQ_EMPLOYEE, "EMP");
    }

    private String nextLeaveNumber() {
        return nextNumber(SEQ_LEAVE, "LV");
    }

    private String nextPayrollPeriodCode() {
        return nextNumber(SEQ_PAYROLL_PERIOD, "PR");
    }

    private String nextPayrollPaymentNumber() {
        return nextNumber(SEQ_PAYROLL_PAYMENT, "PAY");
    }

    private String nextPayslipNumber() {
        return nextNumber(SEQ_PAYSLIP, "PS");
    }

    /** Callers are already inside a transaction, so the row lock holds until commit. */
    private String nextNumber(String sequenceName, String prefix) {
        HrNumberSequence seq = sequenceRepository.findByNameForUpdate(sequenceName)
                .orElseGet(() -> {
                    HrNumberSequence fresh = new HrNumberSequence();
                    fresh.setName(sequenceName);
                    fresh.setNextValue(1L);
                    return sequenceRepository.saveAndFlush(fresh);
                });
        long value = seq.getNextValue();
        seq.setNextValue(value + 1);
        sequenceRepository.save(seq);
        return String.format("%s-%06d", prefix, value);
    }

    // ─── Small helpers ───────────────────────────────────────────

    private String normalizeCode(String code, String fallbackName) {
        if (code != null && !code.isBlank()) {
            return code.trim().toUpperCase().replaceAll("\\s+", "_");
        }
        if (fallbackName == null || fallbackName.isBlank()) {
            throw new BusinessException("A code or name is required");
        }
        return fallbackName.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }

    private BigDecimal scale(BigDecimal v) {
        return (v != null ? v : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
