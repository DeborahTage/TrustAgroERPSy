package com.trustagro.training.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock TrainingProgramRepository programRepository;
    @Mock TrainingSessionRepository sessionRepository;
    @Mock TrainingRegistrationRepository registrationRepository;
    @Mock TrainingPaymentRepository paymentRepository;
    @Mock TrainingAttendanceRepository attendanceRepository;
    @Mock TrainingCertificateRepository certificateRepository;
    @Mock CrmClientRepository clientRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock FinanceService financeService;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;

    @InjectMocks TrainingService service;

    private final AtomicLong ids = new AtomicLong(1);
    private User user;
    private CrmClient client;
    private TrainingProgram program;
    private TrainingSession session;

    @BeforeEach
    void setUp() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("training@test.com", "x");
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);

        user = new User();
        user.setEmail("training@test.com");
        user.setFullName("Coordinator");
        lenient().when(userRepository.findByEmail("training@test.com")).thenReturn(Optional.of(user));

        client = new CrmClient();
        setId(client, 5L);
        client.setClientName("Abebe Kebede");
        client.setPhone("0911000000");
        client.setStatus(ClientStatus.ACTIVE);

        program = new TrainingProgram();
        setId(program, 1L);
        program.setProgramCode("APFM");
        program.setTitle("Advanced Poultry Farm Management");
        program.setPrice(new BigDecimal("10000.00"));
        program.setCurrency("ETB");
        program.setStatus(ProgramStatus.REGISTRATION_OPEN);
        program.setCategory(TrainingCategory.AGRICULTURAL);
        program.setDeliveryMode(DeliveryMode.ONSITE);

        session = new TrainingSession();
        setId(session, 10L);
        session.setProgram(program);
        session.setSessionCode("APFM-SEP-2026");
        session.setStartDate(LocalDate.now().plusDays(7));
        session.setEndDate(LocalDate.now().plusDays(9));
        session.setCapacity(30);
        session.setConfirmedCount(0);
        session.setStatus(SessionStatus.SCHEDULED);

        lenient().when(programRepository.save(any())).thenAnswer(inv -> {
            TrainingProgram p = inv.getArgument(0);
            if (p.getId() == null) setId(p, ids.getAndIncrement());
            return p;
        });
        lenient().when(sessionRepository.save(any())).thenAnswer(inv -> {
            TrainingSession s = inv.getArgument(0);
            if (s.getId() == null) setId(s, ids.getAndIncrement());
            return s;
        });
        lenient().when(registrationRepository.save(any())).thenAnswer(inv -> {
            TrainingRegistration r = inv.getArgument(0);
            if (r.getId() == null) setId(r, ids.getAndIncrement());
            return r;
        });
        lenient().when(paymentRepository.save(any())).thenAnswer(inv -> {
            TrainingPayment p = inv.getArgument(0);
            if (p.getId() == null) setId(p, ids.getAndIncrement());
            return p;
        });
        lenient().when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) setId(i, 50L);
            return i;
        });
        lenient().when(attendanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(certificateRepository.save(any())).thenAnswer(inv -> {
            TrainingCertificate c = inv.getArgument(0);
            if (c.getId() == null) setId(c, ids.getAndIncrement());
            return c;
        });
        lenient().when(clientRepository.save(any())).thenAnswer(inv -> {
            CrmClient c = inv.getArgument(0);
            if (c.getId() == null) setId(c, ids.getAndIncrement());
            return c;
        });
        lenient().when(financeService.postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    FinanceTransaction tx = new FinanceTransaction();
                    setId(tx, 77L);
                    return tx;
                });
        lenient().when(financeService.postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    FinanceTransaction tx = new FinanceTransaction();
                    setId(tx, 78L);
                    return tx;
                });
        lenient().when(sessionRepository.findByProgramIdOrderByStartDateAsc(any())).thenReturn(List.of());
        lenient().when(paymentRepository.findByRegistrationIdOrderByPaymentDateAsc(any())).thenReturn(List.of());
        lenient().when(attendanceRepository.findByRegistrationIdOrderBySessionDateAsc(any())).thenReturn(List.of());
        lenient().when(certificateRepository.findByRegistrationId(any())).thenReturn(Optional.empty());
        lenient().when(registrationRepository.count()).thenReturn(0L);
        lenient().when(programRepository.count()).thenReturn(0L);
        lenient().when(certificateRepository.count()).thenReturn(0L);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAndPublishProgram() {
        when(programRepository.existsByProgramCode(any())).thenReturn(false);
        TrainingProgramRequest req = new TrainingProgramRequest();
        req.setTitle("Vet First Aid");
        req.setPrice(new BigDecimal("5000"));
        req.setCategory(TrainingCategory.VETERINARY);

        var created = service.createProgram(req);
        assertEquals(ProgramStatus.DRAFT, created.getStatus());
        verify(auditService).log(eq("PROGRAM_CREATED"), eq("TRAINING"), eq("TrainingProgram"), any(), any(), any());

        when(programRepository.findById(created.getId())).thenReturn(Optional.of(program));
        program.setStatus(ProgramStatus.DRAFT);
        setId(program, created.getId());
        var published = service.transitionProgram(created.getId(), ProgramStatus.PUBLISHED);
        assertEquals(ProgramStatus.PUBLISHED, published.getStatus());
        verify(auditService).log(eq("PROGRAM_PUBLISHED"), eq("TRAINING"), eq("TrainingProgram"), any(), any(), any());
    }

    @Test
    void createSessionUnderProgram() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        when(sessionRepository.existsBySessionCode(any())).thenReturn(false);
        TrainingSessionRequest req = new TrainingSessionRequest();
        req.setProgramId(1L);
        req.setSessionCode("APFM-SEP-2026");
        req.setStartDate(LocalDate.now().plusDays(3));
        req.setCapacity(30);
        var sessionRes = service.createSession(req);
        assertEquals(30, sessionRes.getCapacity());
        assertEquals(0, sessionRes.getConfirmedCount());
        verify(auditService).log(eq("SESSION_CREATED"), eq("TRAINING"), eq("TrainingSession"), any(), any(), any());
    }

    @Test
    void duplicateRegistrationRejected() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(clientRepository.findById(5L)).thenReturn(Optional.of(client));
        when(registrationRepository.existsBySessionIdAndClientId(10L, 5L)).thenReturn(true);
        TrainingRegistrationRequest req = baseRegRequest();
        assertThrows(BusinessException.class, () -> service.register(req));
    }

    @Test
    void capacityEnforcedOnConfirm() {
        TrainingRegistration reg = pendingRegistration();
        session.setConfirmedCount(30);
        session.setCapacity(30);
        when(registrationRepository.findById(reg.getId())).thenReturn(Optional.of(reg));
        when(sessionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(session));
        assertThrows(BusinessException.class, () -> service.confirmRegistration(reg.getId()));
    }

    @Test
    void confirmCreatesInvoiceAndRevenueNotCash() {
        TrainingRegistration reg = pendingRegistration();
        when(registrationRepository.findById(reg.getId())).thenReturn(Optional.of(reg));
        when(sessionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(session));
        when(invoiceRepository.findBySourceModuleAndSourceId(any(), any())).thenReturn(Optional.empty());

        var confirmed = service.confirmRegistration(reg.getId());
        assertEquals(RegistrationStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(1, session.getConfirmedCount());

        ArgumentCaptor<TransactionType> typeCap = ArgumentCaptor.forClass(TransactionType.class);
        verify(financeService).postTransaction(typeCap.capture(), eq(new BigDecimal("10000.00")),
                eq(BusinessUnit.TRAINING), eq("1200"), eq("4500"), any(), eq("TRAINING"),
                eq("INVOICE_REVENUE"), any());
        assertEquals(TransactionType.INCOME, typeCap.getValue());
        verify(auditService).log(eq("REGISTRATION_CONFIRMED"), eq("TRAINING"), eq("TrainingRegistration"), any(), any(), any());
        verify(notificationService, atLeastOnce()).createRoleNotification(any(), any(), any());
    }

    @Test
    void partialPaymentDoesNotAddRevenue() {
        TrainingRegistration reg = confirmedRegistration(new BigDecimal("10000.00"));
        when(registrationRepository.findById(reg.getId())).thenReturn(Optional.of(reg));

        TrainingPaymentRequest pay = new TrainingPaymentRequest();
        pay.setAmount(new BigDecimal("6000"));
        pay.setPaymentMethod(PaymentMethod.CASH);

        var after = service.recordPayment(reg.getId(), pay);
        assertEquals(0, new BigDecimal("6000.00").compareTo(after.getPaidAmount()));
        assertEquals(0, new BigDecimal("4000.00").compareTo(after.getOutstandingAmount()));
        assertEquals(TrainingPaymentStatus.PARTIALLY_PAID, after.getPaymentStatus());

        verify(financeService).postTransaction(eq(TransactionType.TRANSFER), eq(new BigDecimal("6000.00")),
                eq(BusinessUnit.TRAINING), eq("1000"), eq("1200"), any(), eq("TRAINING"),
                eq("PAYMENT"), any(), eq(PaymentMethod.CASH), any());
        verify(financeService, never()).postTransaction(eq(TransactionType.INCOME),
                eq(new BigDecimal("6000.00")), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void fullPaymentMarksPaid() {
        TrainingRegistration reg = confirmedRegistration(new BigDecimal("10000.00"));
        when(registrationRepository.findById(reg.getId())).thenReturn(Optional.of(reg));
        TrainingPaymentRequest pay = new TrainingPaymentRequest();
        pay.setAmount(new BigDecimal("10000"));
        pay.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        var after = service.recordPayment(reg.getId(), pay);
        assertEquals(TrainingPaymentStatus.PAID, after.getPaymentStatus());
        assertEquals(0, after.getOutstandingAmount().compareTo(BigDecimal.ZERO.setScale(2)));
    }

    @Test
    void attendanceCompletionAndCertificate() {
        TrainingRegistration reg = confirmedRegistration(new BigDecimal("10000.00"));
        when(registrationRepository.findById(reg.getId())).thenReturn(Optional.of(reg));
        when(certificateRepository.existsByRegistrationId(reg.getId())).thenReturn(false);

        AttendanceRequest att = new AttendanceRequest();
        att.setSessionDate(LocalDate.now());
        att.setStatus(AttendanceStatus.PRESENT);
        service.recordAttendance(reg.getId(), att);
        verify(auditService).log(eq("ATTENDANCE_RECORDED"), eq("TRAINING"), eq("TrainingRegistration"), any(), any(), any());

        var completed = service.markCompleted(reg.getId());
        assertTrue(completed.isCompleted());
        assertEquals(RegistrationStatus.COMPLETED, completed.getStatus());

        when(registrationRepository.findById(reg.getId())).thenReturn(Optional.of(reg));
        var withCert = service.issueCertificate(reg.getId(), new CertificateIssueRequest());
        assertNotNull(withCert);
        verify(auditService).log(eq("CERTIFICATE_ISSUED"), eq("TRAINING"), eq("TrainingCertificate"), any(), any(), any());

        when(certificateRepository.existsByRegistrationId(reg.getId())).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.issueCertificate(reg.getId(), null));
    }

    @Test
    void certificateNotIssuedWithoutCompletion() {
        TrainingRegistration reg = confirmedRegistration(new BigDecimal("10000.00"));
        when(registrationRepository.findById(reg.getId())).thenReturn(Optional.of(reg));
        assertThrows(BusinessException.class, () -> service.issueCertificate(reg.getId(), null));
    }

    @Test
    void verifyCertificatePublic() {
        TrainingCertificate cert = new TrainingCertificate();
        setId(cert, 1L);
        cert.setCertificateNumber("TRUST-2026-000001");
        cert.setVerificationCode("ABC123XYZ");
        cert.setParticipantName("Abebe Kebede");
        cert.setStatus(CertificateStatus.ISSUED);
        cert.setIssueDate(LocalDate.now());
        cert.setCertificateType(CertificateType.COMPLETION);
        cert.setProgram(program);
        cert.setSession(session);
        when(certificateRepository.findByVerificationCode("ABC123XYZ")).thenReturn(Optional.of(cert));

        var result = service.verifyCertificate("ABC123XYZ");
        assertTrue(result.isValid());
        assertEquals("VALID", result.getMessage());
        assertEquals("Advanced Poultry Farm Management", result.getProgramTitle());
        assertFalse(result.getParticipantName().contains("Kebede")); // masked
    }

    @Test
    void cancelConfirmedFreesCapacity() {
        TrainingRegistration reg = confirmedRegistration(new BigDecimal("10000.00"));
        session.setConfirmedCount(1);
        when(registrationRepository.findById(reg.getId())).thenReturn(Optional.of(reg));
        when(sessionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(session));
        var cancelled = service.cancelRegistration(reg.getId());
        assertEquals(RegistrationStatus.CANCELLED, cancelled.getStatus());
        assertEquals(0, session.getConfirmedCount());
    }

    @Test
    void ensureInvoiceIsIdempotent() {
        TrainingRegistration reg = confirmedRegistration(new BigDecimal("10000.00"));
        when(registrationRepository.findById(reg.getId())).thenReturn(Optional.of(reg));

        var first = service.ensureInvoice(reg.getId());
        var second = service.ensureInvoice(reg.getId());
        assertEquals(first.getInvoiceNumber(), second.getInvoiceNumber());
        verify(financeService, never()).postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void concurrentCapacitySecondConfirmFails() {
        TrainingRegistration reg1 = pendingRegistration();
        TrainingRegistration reg2 = pendingRegistration();
        setId(reg2, 101L);
        reg2.setRegistrationNumber("TR-TEST-2");
        session.setConfirmedCount(29);
        session.setCapacity(30);

        when(registrationRepository.findById(reg1.getId())).thenReturn(Optional.of(reg1));
        when(sessionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(session));
        when(invoiceRepository.findBySourceModuleAndSourceId(any(), any())).thenReturn(Optional.empty());
        service.confirmRegistration(reg1.getId());
        assertEquals(30, session.getConfirmedCount());

        when(registrationRepository.findById(reg2.getId())).thenReturn(Optional.of(reg2));
        when(sessionRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(session));
        assertThrows(BusinessException.class, () -> service.confirmRegistration(reg2.getId()));
    }

    @Test
    void programLifecycleRejectsInvalidJump() {
        when(programRepository.findById(1L)).thenReturn(Optional.of(program));
        program.setStatus(ProgramStatus.DRAFT);
        assertThrows(BusinessException.class,
                () -> service.transitionProgram(1L, ProgramStatus.COMPLETED));
    }

    private TrainingRegistrationRequest baseRegRequest() {
        TrainingRegistrationRequest req = new TrainingRegistrationRequest();
        req.setSessionId(10L);
        req.setClientId(5L);
        req.setParticipantName("Abebe Kebede");
        req.setParticipantPhone("0911000000");
        return req;
    }

    private TrainingRegistration pendingRegistration() {
        TrainingRegistration reg = new TrainingRegistration();
        setId(reg, 100L);
        reg.setRegistrationNumber("TR-TEST");
        reg.setSession(session);
        reg.setClient(client);
        reg.setParticipantName("Abebe Kebede");
        reg.setRegistrationDate(LocalDate.now());
        reg.setStatus(RegistrationStatus.PENDING);
        reg.setPrice(new BigDecimal("10000.00"));
        reg.setDiscountAmount(ZERO());
        reg.setTaxAmount(ZERO());
        reg.setTotalAmount(new BigDecimal("10000.00"));
        reg.setPaidAmount(ZERO());
        reg.setOutstandingAmount(new BigDecimal("10000.00"));
        reg.setPaymentStatus(TrainingPaymentStatus.UNPAID);
        return reg;
    }

    private TrainingRegistration confirmedRegistration(BigDecimal total) {
        TrainingRegistration reg = pendingRegistration();
        reg.setStatus(RegistrationStatus.CONFIRMED);
        reg.setTotalAmount(total);
        reg.setOutstandingAmount(total);
        Invoice inv = new Invoice();
        setId(inv, 50L);
        inv.setInvoiceNumber("TRINV-1");
        inv.setTotalAmount(total);
        inv.setPaidAmount(ZERO());
        inv.setStatus(InvoiceStatus.SENT);
        reg.setInvoice(inv);
        return reg;
    }

    private BigDecimal ZERO() {
        return BigDecimal.ZERO.setScale(2);
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
