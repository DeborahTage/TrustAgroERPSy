package com.trustagro.training.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.crm.entity.ClientStatus;
import com.trustagro.crm.entity.CrmClient;
import com.trustagro.crm.repository.CrmClientRepository;
import com.trustagro.finance.entity.*;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.training.dto.*;
import com.trustagro.training.entity.*;
import com.trustagro.training.repository.*;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TrainingService {

    public static final String MODULE = "TRAINING";
    public static final String SOURCE_MODULE = FinanceSourceTypes.MODULE_TRAINING;

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal TAX_RATE = new BigDecimal("0.15"); // only when applyTax=true (existing config)

    private final TrainingProgramRepository programRepository;
    private final TrainingSessionRepository sessionRepository;
    private final TrainingRegistrationRepository registrationRepository;
    private final TrainingPaymentRepository paymentRepository;
    private final TrainingAttendanceRepository attendanceRepository;
    private final TrainingCertificateRepository certificateRepository;
    private final CrmClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final FinanceService financeService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ─── Dashboard ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        Map<String, Object> dash = new LinkedHashMap<>();
        long activePrograms = programRepository.findAll().stream()
                .filter(p -> p.getStatus() != ProgramStatus.CANCELLED && p.getStatus() != ProgramStatus.DRAFT)
                .count();
        List<TrainingSession> upcoming = sessionRepository.findByStartDateGreaterThanEqualOrderByStartDateAsc(LocalDate.now())
                .stream()
                .filter(s -> s.getStatus() != SessionStatus.CANCELLED && s.getStatus() != SessionStatus.COMPLETED)
                .limit(10)
                .toList();
        dash.put("activePrograms", activePrograms);
        dash.put("upcomingSessions", upcoming.stream().map(this::toSessionResponse).toList());
        dash.put("upcomingSessionCount", upcoming.size());
        dash.put("totalRegistrations", registrationRepository.count());
        dash.put("confirmedParticipants", registrationRepository.countByStatus(RegistrationStatus.CONFIRMED)
                + registrationRepository.countByStatus(RegistrationStatus.COMPLETED));
        dash.put("unpaidRegistrations", registrationRepository.countByPaymentStatus(TrainingPaymentStatus.UNPAID)
                + registrationRepository.countByPaymentStatus(TrainingPaymentStatus.PARTIAL));
        dash.put("outstandingAmount", nvl(registrationRepository.sumOutstanding()));
        dash.put("completedTrainings", registrationRepository.countByCompletedTrue());
        dash.put("certificatesIssued", certificateRepository.countByStatus(CertificateStatus.ISSUED));
        // Authoritative revenue = INCOME postings from TRAINING module (not registration row sums)
        BigDecimal revenue = financeService.getAllTransactions().stream()
                .filter(t -> SOURCE_MODULE.equals(t.getSourceModule()))
                .filter(t -> t.getTransactionType() == TransactionType.INCOME)
                .map(FinanceTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dash.put("revenue", revenue);
        dash.put("registrationTotals", nvl(registrationRepository.sumRegistrationTotals()));
        dash.put("paidCollected", nvl(registrationRepository.sumPaidAmounts()));
        return dash;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reports() {
        Map<String, Object> report = new LinkedHashMap<>();
        BigDecimal revenue = financeService.getAllTransactions().stream()
                .filter(t -> SOURCE_MODULE.equals(t.getSourceModule()))
                .filter(t -> t.getTransactionType() == TransactionType.INCOME)
                .map(FinanceTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.put("trainingRevenueFromFinance", revenue);
        report.put("registrations", registrationRepository.count());
        report.put("confirmed", registrationRepository.countByStatus(RegistrationStatus.CONFIRMED));
        report.put("completed", registrationRepository.countByCompletedTrue());
        report.put("certificatesIssued", certificateRepository.countByStatus(CertificateStatus.ISSUED));
        report.put("attendancePresent", attendanceRepository.findAll().stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count());
        report.put("attendanceAbsent", attendanceRepository.findAll().stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count());
        List<Map<String, Object>> utilization = sessionRepository.findAll().stream().map(s -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sessionCode", s.getSessionCode());
            row.put("capacity", s.getCapacity());
            row.put("confirmedCount", s.getConfirmedCount());
            row.put("utilizationPct", s.getCapacity() != null && s.getCapacity() > 0
                    ? BigDecimal.valueOf(s.getConfirmedCount() * 100.0 / s.getCapacity()).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            return row;
        }).toList();
        report.put("programUtilization", utilization);
        report.put("note", "Revenue is taken from Finance INCOME postings with sourceModule=TRAINING.");
        return report;
    }

    // ─── Programs ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TrainingProgramResponse> listPrograms() {
        return programRepository.findAll().stream()
                .sorted(Comparator.comparing(TrainingProgram::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toProgramResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrainingProgramResponse getProgram(Long id) {
        return toProgramResponse(requireProgram(id));
    }

    @Transactional
    public TrainingProgramResponse createProgram(TrainingProgramRequest req) {
        if (req.getPrice() == null || req.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Program price must be zero or greater");
        }
        String code = req.getProgramCode() != null && !req.getProgramCode().isBlank()
                ? req.getProgramCode().trim().toUpperCase()
                : nextProgramCode();
        if (programRepository.existsByProgramCode(code)) {
            throw new BusinessException("Program code already exists: " + code);
        }
        TrainingProgram p = new TrainingProgram();
        applyProgram(p, req, code);
        p.setStatus(ProgramStatus.DRAFT);
        p.setCreatedBy(currentUser());
        TrainingProgram saved = programRepository.save(p);
        auditService.log("PROGRAM_CREATED", MODULE, "TrainingProgram", saved.getId(), null, saved.getProgramCode());
        return toProgramResponse(saved);
    }

    @Transactional
    public TrainingProgramResponse updateProgram(Long id, TrainingProgramRequest req) {
        TrainingProgram p = requireProgram(id);
        if (!TrainingProgramLifecycle.isEditable(p.getStatus())) {
            throw new BusinessException("Program cannot be edited in status " + p.getStatus());
        }
        String code = req.getProgramCode() != null && !req.getProgramCode().isBlank()
                ? req.getProgramCode().trim().toUpperCase()
                : p.getProgramCode();
        if (!code.equals(p.getProgramCode()) && programRepository.existsByProgramCode(code)) {
            throw new BusinessException("Program code already exists: " + code);
        }
        applyProgram(p, req, code);
        TrainingProgram saved = programRepository.save(p);
        auditService.log("PROGRAM_UPDATED", MODULE, "TrainingProgram", saved.getId(), null, saved.getProgramCode());
        return toProgramResponse(saved);
    }

    @Transactional
    public TrainingProgramResponse transitionProgram(Long id, ProgramStatus target) {
        TrainingProgram p = requireProgram(id);
        TrainingProgramLifecycle.assertTransition(p.getStatus(), target);
        ProgramStatus from = p.getStatus();
        p.setStatus(target);
        TrainingProgram saved = programRepository.save(p);
        String action = target == ProgramStatus.PUBLISHED ? "PROGRAM_PUBLISHED" : "PROGRAM_UPDATED";
        auditService.log(action, MODULE, "TrainingProgram", saved.getId(), from.name(), target.name());
        if (target == ProgramStatus.PUBLISHED || target == ProgramStatus.REGISTRATION_OPEN) {
            notificationService.createRoleNotification(
                    "Training program " + target.name().toLowerCase().replace('_', ' '),
                    saved.getTitle() + " (" + saved.getProgramCode() + ")",
                    Role.TRAINING_COORDINATOR);
            notificationService.createRoleNotification(
                    "Training program update",
                    saved.getTitle() + " is now " + target,
                    Role.CRM_MANAGER);
        }
        return toProgramResponse(saved);
    }

    // ─── Sessions ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TrainingSessionResponse> listSessions(Long programId) {
        List<TrainingSession> list = programId != null
                ? sessionRepository.findByProgramIdOrderByStartDateAsc(programId)
                : sessionRepository.findAll();
        return list.stream().map(this::toSessionResponse).toList();
    }

    @Transactional(readOnly = true)
    public TrainingSessionResponse getSession(Long id) {
        return toSessionResponse(requireSession(id));
    }

    @Transactional
    public TrainingSessionResponse createSession(TrainingSessionRequest req) {
        TrainingProgram program = requireProgram(req.getProgramId());
        if (program.getStatus() == ProgramStatus.CANCELLED || program.getStatus() == ProgramStatus.COMPLETED) {
            throw new BusinessException("Cannot create session for program in status " + program.getStatus());
        }
        if (req.getCapacity() == null || req.getCapacity() <= 0) {
            throw new BusinessException("Session capacity must be greater than zero");
        }
        String code = req.getSessionCode() != null && !req.getSessionCode().isBlank()
                ? req.getSessionCode().trim().toUpperCase()
                : nextSessionCode(program);
        if (sessionRepository.existsBySessionCode(code)) {
            throw new BusinessException("Session code already exists: " + code);
        }
        TrainingSession s = new TrainingSession();
        s.setProgram(program);
        s.setSessionCode(code);
        s.setStartDate(req.getStartDate());
        s.setEndDate(req.getEndDate() != null ? req.getEndDate() : req.getStartDate());
        s.setVenue(req.getVenue() != null ? req.getVenue() : program.getLocation());
        s.setCapacity(req.getCapacity());
        s.setConfirmedCount(0);
        s.setTrainerName(req.getTrainerName() != null ? req.getTrainerName() : program.getTrainerName());
        s.setNotes(req.getNotes());
        s.setStatus(SessionStatus.SCHEDULED);
        TrainingSession saved = sessionRepository.save(s);
        auditService.log("SESSION_CREATED", MODULE, "TrainingSession", saved.getId(), null, saved.getSessionCode());
        return toSessionResponse(saved);
    }

    // ─── Registrations ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TrainingRegistrationResponse> listRegistrations(Long sessionId, Long clientId) {
        List<TrainingRegistration> list;
        if (sessionId != null) {
            list = registrationRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
        } else if (clientId != null) {
            list = registrationRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        } else {
            list = registrationRepository.findAll();
        }
        return list.stream().map(r -> toRegistrationResponse(r, false)).toList();
    }

    @Transactional(readOnly = true)
    public TrainingRegistrationResponse getRegistration(Long id) {
        return toRegistrationResponse(requireRegistration(id), true);
    }

    @Transactional
    public TrainingRegistrationResponse register(TrainingRegistrationRequest req) {
        TrainingSession session = requireSession(req.getSessionId());
        TrainingProgram program = session.getProgram();
        if (program.getStatus() != ProgramStatus.REGISTRATION_OPEN
                && program.getStatus() != ProgramStatus.PUBLISHED
                && program.getStatus() != ProgramStatus.IN_PROGRESS) {
            throw new BusinessException("Registration is not open for this program (" + program.getStatus() + ")");
        }
        if (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.COMPLETED) {
            throw new BusinessException("Cannot register for session in status " + session.getStatus());
        }

        CrmClient client = resolveOrCreateClient(req);
        if (registrationRepository.existsBySessionIdAndClientId(session.getId(), client.getId())) {
            throw new BusinessException("Participant already registered for this session");
        }

        BigDecimal price = nvl(program.getPrice());
        BigDecimal discount = req.getDiscountAmount() != null ? req.getDiscountAmount().max(ZERO) : ZERO;
        if (discount.compareTo(price) > 0) {
            throw new BusinessException("Discount cannot exceed program price");
        }
        BigDecimal subtotal = price.subtract(discount).max(ZERO);
        BigDecimal tax = req.isApplyTax()
                ? subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP)
                : ZERO;
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        TrainingRegistration reg = new TrainingRegistration();
        reg.setRegistrationNumber(nextRegistrationNumber());
        reg.setSession(session);
        reg.setClient(client);
        reg.setParticipantName(req.getParticipantName().trim());
        reg.setParticipantPhone(req.getParticipantPhone());
        reg.setParticipantEmail(req.getParticipantEmail());
        reg.setOrganization(req.getOrganization());
        reg.setJobTitle(req.getJobTitle());
        reg.setRegistrationDate(LocalDate.now());
        reg.setStatus(RegistrationStatus.PENDING);
        reg.setPrice(price);
        reg.setDiscountAmount(discount);
        reg.setTaxAmount(tax);
        reg.setTotalAmount(total);
        reg.setPaidAmount(ZERO);
        reg.setOutstandingAmount(total);
        reg.setPaymentStatus(TrainingPaymentStatus.UNPAID);
        reg.setNotes(req.getNotes());
        reg.setRegisteredBy(currentUser());

        TrainingRegistration saved = registrationRepository.save(reg);
        auditService.log("REGISTRATION_CREATED", MODULE, "TrainingRegistration", saved.getId(),
                null, saved.getRegistrationNumber());
        notificationService.createRoleNotification(
                "Training registration created",
                saved.getRegistrationNumber() + " — " + saved.getParticipantName() + " / " + session.getSessionCode(),
                Role.TRAINING_COORDINATOR);
        return toRegistrationResponse(saved, false);
    }

    /**
     * Confirm registration under pessimistic lock on the session row to enforce capacity.
     * Creates invoice + recognizes revenue (AR), never cash until payment.
     */
    @Transactional
    public TrainingRegistrationResponse confirmRegistration(Long id) {
        TrainingRegistration reg = requireRegistration(id);
        TrainingRegistrationLifecycle.assertTransition(reg.getStatus(), RegistrationStatus.CONFIRMED);

        TrainingSession session = sessionRepository.findByIdForUpdate(reg.getSession().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (session.getConfirmedCount() >= session.getCapacity()) {
            throw new BusinessException("Session is at full capacity (" + session.getCapacity() + ")");
        }

        if (reg.getInvoice() == null) {
            if (invoiceRepository.findBySourceModuleAndSourceId(SOURCE_MODULE, reg.getId()).isPresent()) {
                throw new BusinessException("Invoice already exists for registration " + reg.getRegistrationNumber());
            }
            createInvoiceAndPostRevenue(reg);
        }

        session.setConfirmedCount(session.getConfirmedCount() + 1);
        sessionRepository.save(session);

        reg.setStatus(RegistrationStatus.CONFIRMED);
        TrainingRegistration saved = registrationRepository.save(reg);

        auditService.log("REGISTRATION_CONFIRMED", MODULE, "TrainingRegistration", saved.getId(),
                RegistrationStatus.PENDING.name(), RegistrationStatus.CONFIRMED.name());
        notificationService.createRoleNotification(
                "Training registration confirmed",
                saved.getRegistrationNumber() + " confirmed for " + session.getSessionCode(),
                Role.TRAINING_COORDINATOR);
        notificationService.createRoleNotification(
                "Training invoice issued",
                "Invoice for " + saved.getRegistrationNumber() + " — " + saved.getTotalAmount() + " ETB",
                Role.FINANCE);
        return toRegistrationResponse(saved, true);
    }

    @Transactional
    public TrainingRegistrationResponse cancelRegistration(Long id) {
        TrainingRegistration reg = requireRegistration(id);
        if (reg.getStatus() == RegistrationStatus.CANCELLED) {
            return toRegistrationResponse(reg, true);
        }
        if (reg.getStatus() == RegistrationStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel a completed registration");
        }
        if (reg.getPaidAmount() != null && reg.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Cannot cancel registration with payments; reverse payments first");
        }

        RegistrationStatus from = reg.getStatus();
        TrainingRegistrationLifecycle.assertTransition(from, RegistrationStatus.CANCELLED);

        if (from == RegistrationStatus.CONFIRMED) {
            TrainingSession session = sessionRepository.findByIdForUpdate(reg.getSession().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
            session.setConfirmedCount(Math.max(0, session.getConfirmedCount() - 1));
            sessionRepository.save(session);
            if (reg.getInvoice() != null && reg.getInvoice().getStatus() != InvoiceStatus.CANCELLED
                    && (reg.getInvoice().getPaidAmount() == null
                    || reg.getInvoice().getPaidAmount().compareTo(BigDecimal.ZERO) == 0)) {
                Invoice inv = reg.getInvoice();
                inv.setStatus(InvoiceStatus.CANCELLED);
                invoiceRepository.save(inv);
            }
        }

        reg.setStatus(RegistrationStatus.CANCELLED);
        TrainingRegistration saved = registrationRepository.save(reg);
        auditService.log("REGISTRATION_CANCELLED", MODULE, "TrainingRegistration", saved.getId(),
                from.name(), RegistrationStatus.CANCELLED.name());
        return toRegistrationResponse(saved, true);
    }

    @Transactional
    public TrainingRegistrationResponse recordPayment(Long id, TrainingPaymentRequest req) {
        TrainingRegistration reg = requireRegistration(id);
        if (reg.getStatus() != RegistrationStatus.CONFIRMED && reg.getStatus() != RegistrationStatus.COMPLETED) {
            throw new BusinessException("Payments require a confirmed registration");
        }
        if (reg.getInvoice() == null) {
            throw new BusinessException("Cannot record payment before invoice exists");
        }
        if (req.getPaymentMethod() == PaymentMethod.CREDIT) {
            throw new BusinessException("CREDIT is not a receipt method. Use CASH, BANK_TRANSFER, or MOBILE_MONEY");
        }
        BigDecimal amount = req.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be greater than zero");
        }
        if (amount.compareTo(reg.getOutstandingAmount()) > 0) {
            throw new BusinessException("Payment exceeds outstanding amount of " + reg.getOutstandingAmount());
        }

        String debit = req.getPaymentMethod() == PaymentMethod.CASH
                ? AccountCode.ASSET_CASH.getCode()
                : AccountCode.ASSET_BANK.getCode();

        TrainingPayment payment = new TrainingPayment();
        payment.setRegistration(reg);
        payment.setInvoiceId(reg.getInvoice().getId());
        payment.setAmount(amount);
        payment.setPaymentDate(req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now());
        payment.setPaymentMethod(req.getPaymentMethod());
        payment.setNotes(req.getNotes());
        payment.setRecordedBy(currentUser());
        payment = paymentRepository.save(payment);

        // Settlement only — never additional revenue
        FinanceTransaction tx = financeService.postTransaction(
                TransactionType.TRANSFER, amount, BusinessUnit.TRAINING,
                debit, AccountCode.ASSET_RECEIVABLE.getCode(),
                "Training payment " + reg.getRegistrationNumber() + " (" + req.getPaymentMethod() + ")",
                SOURCE_MODULE, FinanceSourceTypes.REF_PAYMENT, payment.getId(),
                req.getPaymentMethod(), payment.getPaymentDate());
        payment.setFinanceTransactionId(tx.getId());
        paymentRepository.save(payment);

        BigDecimal newPaid = nvl(reg.getPaidAmount()).add(amount);
        BigDecimal outstanding = nvl(reg.getTotalAmount()).subtract(newPaid).max(ZERO);
        reg.setPaidAmount(newPaid);
        reg.setOutstandingAmount(outstanding);
        reg.setPaymentStatus(outstanding.compareTo(BigDecimal.ZERO) == 0
                ? TrainingPaymentStatus.PAID : TrainingPaymentStatus.PARTIAL);

        Invoice invoice = reg.getInvoice();
        invoice.setPaidAmount(nvl(invoice.getPaidAmount()).add(amount));
        invoice.setStatus(outstanding.compareTo(BigDecimal.ZERO) == 0
                ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        invoiceRepository.save(invoice);

        TrainingRegistration saved = registrationRepository.save(reg);
        auditService.log("PAYMENT_CREATED", MODULE, "TrainingRegistration", saved.getId(),
                amount.toPlainString(), saved.getPaymentStatus().name());
        notificationService.createRoleNotification(
                "Training payment received",
                saved.getRegistrationNumber() + " paid " + amount.toPlainString() + " ETB",
                Role.FINANCE);
        notificationService.createRoleNotification(
                "Training payment received",
                saved.getRegistrationNumber() + " paid " + amount.toPlainString() + " ETB",
                Role.TRAINING_COORDINATOR);
        return toRegistrationResponse(saved, true);
    }

    // ─── Attendance / Completion / Certificate ───────────────────

    @Transactional
    public TrainingRegistrationResponse recordAttendance(Long registrationId, AttendanceRequest req) {
        TrainingRegistration reg = requireRegistration(registrationId);
        if (reg.getStatus() != RegistrationStatus.CONFIRMED && reg.getStatus() != RegistrationStatus.COMPLETED) {
            throw new BusinessException("Attendance requires a confirmed registration");
        }
        TrainingAttendance att = attendanceRepository
                .findByRegistrationIdAndSessionDate(registrationId, req.getSessionDate())
                .orElseGet(TrainingAttendance::new);
        att.setRegistration(reg);
        att.setSession(reg.getSession());
        att.setSessionDate(req.getSessionDate());
        att.setStatus(req.getStatus());
        if (req.getStatus() == AttendanceStatus.PRESENT && att.getCheckInTime() == null) {
            att.setCheckInTime(LocalDateTime.now());
        }
        att.setRecordedBy(currentUser());
        att.setNotes(req.getNotes());
        attendanceRepository.save(att);
        auditService.log("ATTENDANCE_RECORDED", MODULE, "TrainingRegistration", reg.getId(),
                req.getSessionDate().toString(), req.getStatus().name());
        return toRegistrationResponse(reg, true);
    }

    @Transactional
    public TrainingRegistrationResponse markCompleted(Long id) {
        TrainingRegistration reg = requireRegistration(id);
        TrainingRegistrationLifecycle.assertTransition(reg.getStatus(), RegistrationStatus.COMPLETED);
        reg.setStatus(RegistrationStatus.COMPLETED);
        reg.setCompleted(true);
        reg.setCompletedAt(LocalDateTime.now());
        reg.setCompletedBy(currentUser());
        TrainingRegistration saved = registrationRepository.save(reg);
        auditService.log("TRAINING_COMPLETED", MODULE, "TrainingRegistration", saved.getId(),
                RegistrationStatus.CONFIRMED.name(), RegistrationStatus.COMPLETED.name());
        notificationService.createRoleNotification(
                "Training completed",
                saved.getRegistrationNumber() + " — " + saved.getParticipantName() + " marked completed",
                Role.TRAINING_COORDINATOR);
        return toRegistrationResponse(saved, true);
    }

    @Transactional
    public TrainingRegistrationResponse issueCertificate(Long registrationId, CertificateIssueRequest req) {
        TrainingRegistration reg = requireRegistration(registrationId);
        if (!reg.isCompleted() || reg.getStatus() != RegistrationStatus.COMPLETED) {
            throw new BusinessException("Certificate requires a completed registration");
        }
        if (certificateRepository.existsByRegistrationId(registrationId)) {
            throw new BusinessException("Certificate already issued for this registration");
        }
        TrainingCertificate cert = new TrainingCertificate();
        cert.setCertificateNumber(nextCertificateNumber());
        cert.setVerificationCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        cert.setRegistration(reg);
        cert.setProgram(reg.getSession().getProgram());
        cert.setSession(reg.getSession());
        cert.setParticipantName(reg.getParticipantName());
        cert.setCertificateType(req != null && req.getCertificateType() != null
                ? req.getCertificateType() : CertificateType.COMPLETION);
        cert.setStatus(CertificateStatus.ISSUED);
        cert.setIssueDate(LocalDate.now());
        cert.setIssuedBy(currentUser());
        certificateRepository.save(cert);
        auditService.log("CERTIFICATE_ISSUED", MODULE, "TrainingCertificate", cert.getId(),
                null, cert.getCertificateNumber());
        notificationService.createRoleNotification(
                "Training certificate issued",
                cert.getCertificateNumber() + " for " + reg.getParticipantName(),
                Role.TRAINING_COORDINATOR);
        return toRegistrationResponse(reg, true);
    }

    @Transactional
    public TrainingRegistrationResponse revokeCertificate(Long registrationId) {
        TrainingCertificate cert = certificateRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found"));
        if (cert.getStatus() == CertificateStatus.REVOKED) {
            return toRegistrationResponse(cert.getRegistration(), true);
        }
        cert.setStatus(CertificateStatus.REVOKED);
        certificateRepository.save(cert);
        auditService.log("CERTIFICATE_REVOKED", MODULE, "TrainingCertificate", cert.getId(),
                CertificateStatus.ISSUED.name(), CertificateStatus.REVOKED.name());
        return toRegistrationResponse(cert.getRegistration(), true);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCertificates() {
        return certificateRepository.findAll().stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("certificateNumber", c.getCertificateNumber());
            m.put("verificationCode", c.getVerificationCode());
            m.put("participantName", c.getParticipantName());
            m.put("programTitle", c.getProgram().getTitle());
            m.put("sessionCode", c.getSession().getSessionCode());
            m.put("status", c.getStatus().name());
            m.put("issueDate", c.getIssueDate());
            m.put("certificateType", c.getCertificateType().name());
            m.put("registrationId", c.getRegistration().getId());
            return m;
        }).toList();
    }

    @Transactional(readOnly = true)
    public CertificateVerificationResponse verifyCertificate(String code) {
        if (code == null || code.isBlank()) {
            return CertificateVerificationResponse.builder().valid(false).message("Verification code required").build();
        }
        return certificateRepository.findByVerificationCode(code.trim().toUpperCase())
                .or(() -> certificateRepository.findByCertificateNumber(code.trim().toUpperCase()))
                .map(c -> {
                    boolean valid = c.getStatus() == CertificateStatus.ISSUED;
                    return CertificateVerificationResponse.builder()
                            .valid(valid)
                            .status(c.getStatus().name())
                            .certificateNumber(c.getCertificateNumber())
                            .participantName(maskName(c.getParticipantName()))
                            .programTitle(c.getProgram().getTitle())
                            .sessionCode(c.getSession().getSessionCode())
                            .issueDate(c.getIssueDate())
                            .certificateType(c.getCertificateType().name())
                            .message(valid ? "VALID" : c.getStatus().name())
                            .build();
                })
                .orElse(CertificateVerificationResponse.builder()
                        .valid(false)
                        .message("Certificate not found")
                        .build());
    }

    // ─── Finance helpers ─────────────────────────────────────────

    private void createInvoiceAndPostRevenue(TrainingRegistration reg) {
        TrainingProgram program = reg.getSession().getProgram();
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(nextInvoiceNumber());
        invoice.setInvoiceType(InvoiceType.TRAINING);
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setClientId(reg.getClient().getId());
        invoice.setClientName(reg.getParticipantName());
        invoice.setSubtotal(reg.getPrice().subtract(reg.getDiscountAmount()).max(ZERO));
        invoice.setDiscountAmount(reg.getDiscountAmount());
        invoice.setVatAmount(reg.getTaxAmount());
        invoice.setTotalAmount(reg.getTotalAmount());
        invoice.setPaidAmount(ZERO);
        invoice.setBusinessUnit(BusinessUnit.TRAINING);
        invoice.setSourceModule(SOURCE_MODULE);
        invoice.setSourceType(FinanceSourceTypes.REF_INVOICE_REVENUE);
        invoice.setSourceId(reg.getId());
        invoice.setDueDate(LocalDate.now().plusDays(14));

        InvoiceItem item = new InvoiceItem();
        item.setDescription("Training: " + program.getTitle() + " / " + reg.getSession().getSessionCode());
        item.setQuantity(1.0);
        item.setUnitPrice(invoice.getSubtotal());
        item.setLineTotal(invoice.getSubtotal());
        item.setAccountCode(AccountCode.REV_TRAINING);
        invoice.addItem(item);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        reg.setInvoice(savedInvoice);

        BigDecimal netRevenue = invoice.getSubtotal();
        if (netRevenue.compareTo(BigDecimal.ZERO) > 0) {
            financeService.postTransaction(
                    TransactionType.INCOME, netRevenue, BusinessUnit.TRAINING,
                    AccountCode.ASSET_RECEIVABLE.getCode(), AccountCode.REV_TRAINING.getCode(),
                    "Training invoice " + savedInvoice.getInvoiceNumber(),
                    SOURCE_MODULE, FinanceSourceTypes.REF_INVOICE_REVENUE, savedInvoice.getId());
        }
        if (reg.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            financeService.postTransaction(
                    TransactionType.TRANSFER, reg.getTaxAmount(), BusinessUnit.TRAINING,
                    AccountCode.ASSET_RECEIVABLE.getCode(), AccountCode.LIABILITY_VAT.getCode(),
                    "VAT on training invoice " + savedInvoice.getInvoiceNumber(),
                    SOURCE_MODULE, FinanceSourceTypes.REF_INVOICE_VAT, savedInvoice.getId());
        }
    }

    private CrmClient resolveOrCreateClient(TrainingRegistrationRequest req) {
        if (req.getClientId() != null) {
            return clientRepository.findById(req.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("CRM client not found"));
        }
        String phone = req.getParticipantPhone() != null ? req.getParticipantPhone().trim() : null;
        if (phone != null && !phone.isBlank()) {
            Optional<CrmClient> existing = clientRepository.findAll().stream()
                    .filter(c -> phone.equalsIgnoreCase(c.getPhone() != null ? c.getPhone().trim() : ""))
                    .findFirst();
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        CrmClient client = new CrmClient();
        client.setClientName(req.getParticipantName().trim());
        client.setPhone(phone);
        client.setLocation(req.getOrganization());
        client.setStatus(ClientStatus.ACTIVE);
        return clientRepository.save(client);
    }

    // ─── Mapping ─────────────────────────────────────────────────

    private TrainingProgramResponse toProgramResponse(TrainingProgram p) {
        int sessions = sessionRepository.findByProgramIdOrderByStartDateAsc(p.getId()).size();
        return TrainingProgramResponse.builder()
                .id(p.getId())
                .programCode(p.getProgramCode())
                .title(p.getTitle())
                .description(p.getDescription())
                .category(p.getCategory())
                .objectives(p.getObjectives())
                .durationHours(p.getDurationHours())
                .deliveryMode(p.getDeliveryMode())
                .location(p.getLocation())
                .trainerName(p.getTrainerName())
                .defaultCapacity(p.getDefaultCapacity())
                .price(p.getPrice())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .registrationDeadline(p.getRegistrationDeadline())
                .createdAt(p.getCreatedAt())
                .sessionCount(sessions)
                .build();
    }

    private TrainingSessionResponse toSessionResponse(TrainingSession s) {
        return TrainingSessionResponse.builder()
                .id(s.getId())
                .programId(s.getProgram().getId())
                .programCode(s.getProgram().getProgramCode())
                .programTitle(s.getProgram().getTitle())
                .sessionCode(s.getSessionCode())
                .startDate(s.getStartDate())
                .endDate(s.getEndDate())
                .venue(s.getVenue())
                .capacity(s.getCapacity())
                .confirmedCount(s.getConfirmedCount())
                .remainingCapacity(s.remainingCapacity())
                .trainerName(s.getTrainerName())
                .status(s.getStatus())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private TrainingRegistrationResponse toRegistrationResponse(TrainingRegistration r, boolean detail) {
        TrainingRegistrationResponse.TrainingRegistrationResponseBuilder b = TrainingRegistrationResponse.builder()
                .id(r.getId())
                .registrationNumber(r.getRegistrationNumber())
                .sessionId(r.getSession().getId())
                .sessionCode(r.getSession().getSessionCode())
                .programId(r.getSession().getProgram().getId())
                .programTitle(r.getSession().getProgram().getTitle())
                .clientId(r.getClient().getId())
                .participantName(r.getParticipantName())
                .participantPhone(r.getParticipantPhone())
                .participantEmail(r.getParticipantEmail())
                .organization(r.getOrganization())
                .jobTitle(r.getJobTitle())
                .registrationDate(r.getRegistrationDate())
                .status(r.getStatus())
                .price(r.getPrice())
                .discountAmount(r.getDiscountAmount())
                .taxAmount(r.getTaxAmount())
                .totalAmount(r.getTotalAmount())
                .paidAmount(r.getPaidAmount())
                .outstandingAmount(r.getOutstandingAmount())
                .paymentStatus(r.getPaymentStatus())
                .invoiceId(r.getInvoice() != null ? r.getInvoice().getId() : null)
                .invoiceNumber(r.getInvoice() != null ? r.getInvoice().getInvoiceNumber() : null)
                .completed(r.isCompleted())
                .completedAt(r.getCompletedAt())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt());
        if (detail) {
            b.attendance(attendanceRepository.findByRegistrationIdOrderBySessionDateAsc(r.getId()).stream()
                    .map(a -> TrainingRegistrationResponse.AttendanceResponse.builder()
                            .id(a.getId())
                            .sessionDate(a.getSessionDate())
                            .status(a.getStatus().name())
                            .checkInTime(a.getCheckInTime())
                            .build())
                    .toList());
            b.payments(paymentRepository.findByRegistrationIdOrderByPaymentDateAsc(r.getId()).stream()
                    .map(p -> TrainingRegistrationResponse.PaymentResponse.builder()
                            .id(p.getId())
                            .amount(p.getAmount())
                            .paymentMethod(p.getPaymentMethod().name())
                            .paymentDate(p.getPaymentDate())
                            .financeTransactionId(p.getFinanceTransactionId())
                            .build())
                    .toList());
            certificateRepository.findByRegistrationId(r.getId()).ifPresent(c ->
                    b.certificate(TrainingRegistrationResponse.CertificateResponse.builder()
                            .id(c.getId())
                            .certificateNumber(c.getCertificateNumber())
                            .verificationCode(c.getVerificationCode())
                            .status(c.getStatus().name())
                            .issueDate(c.getIssueDate())
                            .certificateType(c.getCertificateType().name())
                            .build()));
        }
        return b.build();
    }

    private void applyProgram(TrainingProgram p, TrainingProgramRequest req, String code) {
        p.setProgramCode(code);
        p.setTitle(req.getTitle().trim());
        p.setDescription(req.getDescription());
        p.setCategory(req.getCategory() != null ? req.getCategory() : TrainingCategory.OTHER);
        p.setObjectives(req.getObjectives());
        p.setDurationHours(req.getDurationHours());
        p.setDeliveryMode(req.getDeliveryMode() != null ? req.getDeliveryMode() : DeliveryMode.ONSITE);
        p.setLocation(req.getLocation());
        p.setTrainerName(req.getTrainerName());
        p.setDefaultCapacity(req.getDefaultCapacity());
        p.setPrice(req.getPrice().setScale(2, RoundingMode.HALF_UP));
        p.setCurrency(req.getCurrency() != null && !req.getCurrency().isBlank() ? req.getCurrency() : "ETB");
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setRegistrationDeadline(req.getRegistrationDeadline());
    }

    private TrainingProgram requireProgram(Long id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Training program not found"));
    }

    private TrainingSession requireSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Training session not found"));
    }

    private TrainingRegistration requireRegistration(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Training registration not found"));
    }

    private String nextProgramCode() {
        return "TP-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
                + "-" + String.format("%03d", programRepository.count() + 1);
    }

    private String nextSessionCode(TrainingProgram program) {
        long n = sessionRepository.findByProgramIdOrderByStartDateAsc(program.getId()).size() + 1;
        String base = program.getProgramCode().replaceAll("[^A-Z0-9]", "");
        if (base.length() > 8) base = base.substring(0, 8);
        return base + "-" + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)).toUpperCase(Locale.ENGLISH)
                + "-" + LocalDate.now().getYear() + "-" + String.format("%02d", n);
    }

    private String nextRegistrationNumber() {
        return "TR-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", registrationRepository.count() + 1);
    }

    private String nextInvoiceNumber() {
        return "TRINV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String nextCertificateNumber() {
        return "TRUST-" + LocalDate.now().getYear() + "-"
                + String.format("%06d", certificateRepository.count() + 1);
    }

    private String maskName(String name) {
        if (name == null || name.isBlank()) return "—";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].charAt(0) + "***";
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }

    private User currentUser() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }
}
