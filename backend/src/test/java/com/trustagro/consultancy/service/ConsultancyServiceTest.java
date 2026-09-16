package com.trustagro.consultancy.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.consultancy.dto.*;
import com.trustagro.consultancy.entity.*;
import com.trustagro.consultancy.repository.*;
import com.trustagro.crm.entity.ClientStatus;
import com.trustagro.crm.entity.CrmClient;
import com.trustagro.crm.repository.CrmClientRepository;
import com.trustagro.finance.entity.*;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.security.Permissions;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultancyServiceTest {

    @Mock ConsultancyServiceCatalogRepository serviceCatalogRepository;
    @Mock ConsultancyRequestRepository requestRepository;
    @Mock ConsultancyProposalRepository proposalRepository;
    @Mock ConsultancyEngagementRepository engagementRepository;
    @Mock ConsultancyAssignmentRepository assignmentRepository;
    @Mock ConsultancyActivityRepository activityRepository;
    @Mock ConsultancyDeliverableRepository deliverableRepository;
    @Mock ConsultancyContractRepository contractRepository;
    @Mock ConsultancyPaymentRepository paymentRepository;
    @Mock CrmClientRepository clientRepository;
    @Mock com.trustagro.crm.repository.CrmQuotationRepository crmQuotationRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock FinanceService financeService;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;

    @InjectMocks ConsultancyService service;

    private final AtomicLong ids = new AtomicLong(1);
    private User user;
    private User otherUser;
    private CrmClient client;

    @BeforeEach
    void setUp() {
        setAuth(Permissions.CONSULTANCY_APPROVE, Permissions.CONSULTANCY_ASSIGN,
                Permissions.CONSULTANCY_INVOICE, Permissions.CONSULTANCY_PAYMENT,
                Permissions.CONSULTANCY_CREATE, Permissions.CONSULTANCY_UPDATE,
                Permissions.CONSULTANCY_READ, Permissions.CONSULTANCY_ACTIVITY,
                Permissions.CONSULTANCY_DELIVERABLE, Permissions.CONSULTANCY_CONTRACT,
                Permissions.CONSULTANCY_COMPLETE, Permissions.CONSULTANCY_CANCEL);

        user = new User();
        setId(user, 1L);
        user.setEmail("consultant@test.com");
        user.setFullName("Lead Consultant");
        otherUser = new User();
        setId(otherUser, 2L);
        otherUser.setEmail("other@test.com");
        otherUser.setFullName("Other");

        lenient().when(userRepository.findByEmail("consultant@test.com")).thenReturn(Optional.of(user));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        lenient().when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

        client = new CrmClient();
        setId(client, 5L);
        client.setClientName("Farm Client");
        client.setStatus(ClientStatus.ACTIVE);
        lenient().when(clientRepository.findById(5L)).thenReturn(Optional.of(client));

        lenient().when(requestRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(proposalRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(engagementRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(assignmentRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(activityRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(deliverableRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(contractRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(paymentRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) setId(i, 50L);
            return i;
        });
        lenient().when(financeService.postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> tx(77L));
        lenient().when(financeService.postTransaction(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> tx(78L));
        lenient().when(financeService.getAllTransactions()).thenReturn(List.of());
        lenient().when(requestRepository.count()).thenReturn(0L);
        lenient().when(proposalRepository.count()).thenReturn(0L);
        lenient().when(engagementRepository.count()).thenReturn(0L);
        lenient().when(activityRepository.count()).thenReturn(0L);
        lenient().when(deliverableRepository.count()).thenReturn(0L);
        lenient().when(contractRepository.count()).thenReturn(0L);
        lenient().when(paymentRepository.count()).thenReturn(0L);
        lenient().when(paymentRepository.findByEngagementIdOrderByPaymentDateAsc(any())).thenReturn(List.of());
        lenient().when(assignmentRepository.existsByEngagementIdAndConsultantIdAndStatusIn(any(), any(), any()))
                .thenReturn(false);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRequestAssignAndAudit() {
        RequestCreateRequest req = new RequestCreateRequest();
        req.setCustomerId(5L);
        req.setSubject("Farm biosecurity assessment");
        var created = service.createRequest(req);
        assertEquals(RequestStatus.NEW, created.getStatus());
        verify(auditService).log(eq("CONSULTANCY_REQUEST_CREATED"), eq("CONSULTANCY"), eq("ConsultancyRequest"), any(), any(), any());
        verify(notificationService, atLeastOnce()).createRoleNotification(any(), any(), any());

        ConsultancyRequest entity = new ConsultancyRequest();
        setId(entity, created.getId());
        entity.setRequestNumber(created.getRequestNumber());
        entity.setCustomer(client);
        entity.setSubject(created.getSubject());
        entity.setStatus(RequestStatus.NEW);
        when(requestRepository.findById(created.getId())).thenReturn(Optional.of(entity));

        AssignmentRequest assign = new AssignmentRequest();
        assign.setConsultantId(1L);
        var assigned = service.assignRequest(created.getId(), assign);
        assertEquals(RequestStatus.ASSIGNED, assigned.getStatus());
        verify(auditService).log(eq("CONSULTANCY_REQUEST_ASSIGNED"), eq("CONSULTANCY"), eq("ConsultancyRequest"), any(), any(), any());
    }

    @Test
    void proposalAcceptCreatesEngagementAndIsIdempotent() {
        ConsultancyProposal proposal = sentProposal();
        when(proposalRepository.findById(20L)).thenReturn(Optional.of(proposal));

        var accepted = service.acceptProposal(20L);
        assertEquals(ProposalStatus.ACCEPTED, accepted.getStatus());
        assertNotNull(accepted.getConvertedEngagementId());
        verify(engagementRepository, times(1)).save(any());
        verify(auditService).log(eq("CONSULTANCY_PROPOSAL_ACCEPTED"), eq("CONSULTANCY"), eq("ConsultancyProposal"), any(), any(), any());

        var again = service.acceptProposal(20L);
        assertEquals(accepted.getConvertedEngagementId(), again.getConvertedEngagementId());
        verify(engagementRepository, times(1)).save(any());
    }

    @Test
    void engagementTransitionsApproveActivateHoldResume() {
        ConsultancyEngagement e = draftEngagement();
        when(engagementRepository.findById(30L)).thenReturn(Optional.of(e));

        e.setStatus(EngagementStatus.DRAFT);
        service.proposeEngagement(30L);
        assertEquals(EngagementStatus.PROPOSED, e.getStatus());

        service.approveEngagement(30L);
        assertEquals(EngagementStatus.APPROVED, e.getStatus());
        verify(auditService).log(eq("CONSULTANCY_ENGAGEMENT_APPROVED"), eq("CONSULTANCY"), eq("ConsultancyEngagement"), any(), any(), any());

        service.activateEngagement(30L);
        assertEquals(EngagementStatus.ACTIVE, e.getStatus());

        service.holdEngagement(30L);
        assertEquals(EngagementStatus.ON_HOLD, e.getStatus());

        service.resumeEngagement(30L);
        assertEquals(EngagementStatus.ACTIVE, e.getStatus());
    }

    @Test
    void consultantIsolationDeniesUnrelatedEngagement() {
        setAuth(Permissions.CONSULTANCY_READ); // scoped consultant — no broad access
        ConsultancyEngagement e = draftEngagement();
        e.setLeadConsultant(otherUser);
        when(engagementRepository.findById(30L)).thenReturn(Optional.of(e));
        when(assignmentRepository.existsByEngagementIdAndConsultantIdAndStatusIn(eq(30L), eq(1L), any()))
                .thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getEngagement(30L));
        assertTrue(ex.getMessage().contains("Not authorized"));
    }

    @Test
    void activityCreateAndComplete() {
        ConsultancyEngagement e = activeEngagement();
        when(engagementRepository.findById(30L)).thenReturn(Optional.of(e));

        ActivityRequest req = new ActivityRequest();
        req.setEngagementId(30L);
        req.setActivityDate(LocalDate.now());
        req.setDescription("Site visit");
        req.setActivityType(ActivityType.SITE_VISIT);
        var created = service.createActivity(req);
        assertEquals(ActivityStatus.PLANNED, created.getStatus());

        ConsultancyActivity a = new ConsultancyActivity();
        setId(a, created.getId());
        a.setActivityNumber(created.getActivityNumber());
        a.setEngagement(e);
        a.setCustomer(client);
        a.setDescription("Site visit");
        a.setActivityDate(LocalDate.now());
        a.setStatus(ActivityStatus.PLANNED);
        when(activityRepository.findById(created.getId())).thenReturn(Optional.of(a));

        var completed = service.completeActivity(created.getId(), null);
        assertEquals(ActivityStatus.COMPLETED, completed.getStatus());
    }

    @Test
    void deliverableSubmitApproveRejectRework() {
        ConsultancyEngagement e = activeEngagement();
        ConsultancyDeliverable d = new ConsultancyDeliverable();
        setId(d, 40L);
        d.setDeliverableNumber("DEL-1");
        d.setEngagement(e);
        d.setTitle("Assessment report");
        d.setStatus(DeliverableStatus.PENDING);
        d.setBillable(false);
        when(deliverableRepository.findById(40L)).thenReturn(Optional.of(d));

        service.startDeliverable(40L);
        assertEquals(DeliverableStatus.IN_PROGRESS, d.getStatus());
        service.submitDeliverable(40L);
        assertEquals(DeliverableStatus.SUBMITTED, d.getStatus());

        service.approveDeliverable(40L);
        assertEquals(DeliverableStatus.APPROVED, d.getStatus());

        d.setStatus(DeliverableStatus.SUBMITTED);
        service.rejectDeliverable(40L, null);
        assertEquals(DeliverableStatus.IN_PROGRESS, d.getStatus());
    }

    @Test
    void unauthorizedApproveFailsWithoutAuthority() {
        setAuth(Permissions.CONSULTANCY_READ, Permissions.CONSULTANCY_DELIVERABLE);
        assertThrows(BusinessException.class, () -> service.approveDeliverable(40L));
    }

    @Test
    void createContract() {
        ConsultancyEngagement e = activeEngagement();
        when(engagementRepository.findById(30L)).thenReturn(Optional.of(e));
        when(contractRepository.existsByEngagementId(30L)).thenReturn(false);

        ContractRequest req = new ContractRequest();
        req.setEngagementId(30L);
        req.setTitle("Advisory contract");
        req.setContractValue(new BigDecimal("25000"));
        var created = service.createContract(req);
        assertEquals(ContractStatus.DRAFT, created.getStatus());
        verify(auditService).log(eq("CONSULTANCY_CONTRACT_CREATED"), eq("CONSULTANCY"), eq("ConsultancyContract"), any(), any(), any());
    }

    @Test
    void createInvoicePostsIncomeOnceAndIsIdempotent() {
        ConsultancyEngagement e = activeEngagement();
        e.setAgreedAmount(new BigDecimal("10000.00"));
        when(engagementRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(e));
        when(invoiceRepository.findBySourceModuleAndSourceTypeAndSourceId(any(), any(), any()))
                .thenReturn(Optional.empty());

        var first = service.createEngagementInvoice(30L);
        assertNotNull(first.getInvoiceId());
        assertEquals(0, new BigDecimal("10000.00").compareTo(first.getInvoicedAmount()));
        assertEquals(ConsultancyPaymentStatus.UNPAID, first.getPaymentStatus());

        ArgumentCaptor<TransactionType> typeCap = ArgumentCaptor.forClass(TransactionType.class);
        verify(financeService).postTransaction(typeCap.capture(), eq(new BigDecimal("10000.00")),
                eq(BusinessUnit.CONSULTING), eq("1200"), eq("4400"), any(), eq("CONSULTANCY"),
                eq("INVOICE_REVENUE"), any());
        assertEquals(TransactionType.INCOME, typeCap.getValue());
        verify(auditService).log(eq("CONSULTANCY_INVOICE_CREATED"), eq("CONSULTANCY"), eq("ConsultancyEngagement"), any(), any(), any());

        var second = service.createEngagementInvoice(30L);
        assertEquals(first.getInvoiceNumber(), second.getInvoiceNumber());
        verify(financeService, times(1)).postTransaction(eq(TransactionType.INCOME), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void paymentPostsTransferOnlyPartialThenFull() {
        ConsultancyEngagement e = activeEngagement();
        e.setAgreedAmount(new BigDecimal("10000.00"));
        e.setInvoicedAmount(new BigDecimal("10000.00"));
        e.setOutstandingAmount(new BigDecimal("10000.00"));
        e.setPaidAmount(ZERO());
        Invoice inv = new Invoice();
        setId(inv, 50L);
        inv.setInvoiceNumber("CSINV-1");
        inv.setTotalAmount(new BigDecimal("10000.00"));
        inv.setPaidAmount(ZERO());
        inv.setStatus(InvoiceStatus.SENT);
        e.setInvoice(inv);
        when(engagementRepository.findById(30L)).thenReturn(Optional.of(e));

        PaymentRequest pay = new PaymentRequest();
        pay.setAmount(new BigDecimal("6000"));
        pay.setPaymentMethod(PaymentMethod.CASH);
        var after = service.recordPayment(30L, pay);
        assertEquals(ConsultancyPaymentStatus.PARTIALLY_PAID, after.getPaymentStatus());
        assertEquals(0, new BigDecimal("4000.00").compareTo(after.getOutstandingAmount()));

        verify(financeService).postTransaction(eq(TransactionType.TRANSFER), eq(new BigDecimal("6000.00")),
                eq(BusinessUnit.CONSULTING), eq("1000"), eq("1200"), any(), eq("CONSULTANCY"),
                eq("PAYMENT"), any(), eq(PaymentMethod.CASH), any());
        verify(financeService, never()).postTransaction(eq(TransactionType.INCOME),
                eq(new BigDecimal("6000.00")), any(), any(), any(), any(), any(), any(), any(), any(), any());

        PaymentRequest full = new PaymentRequest();
        full.setAmount(new BigDecimal("4000"));
        full.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        var paid = service.recordPayment(30L, full);
        assertEquals(ConsultancyPaymentStatus.PAID, paid.getPaymentStatus());
        assertEquals(0, paid.getOutstandingAmount().compareTo(ZERO()));
    }

    @Test
    void cancelledCannotInvoice() {
        ConsultancyEngagement e = activeEngagement();
        e.setStatus(EngagementStatus.CANCELLED);
        when(engagementRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(e));
        assertThrows(BusinessException.class, () -> service.createEngagementInvoice(30L));
    }

    @Test
    void draftCannotInvoice() {
        ConsultancyEngagement e = draftEngagement();
        when(engagementRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(e));
        assertThrows(BusinessException.class, () -> service.createEngagementInvoice(30L));
    }

    @Test
    void completedEngagementCanStillBeInvoiced() {
        ConsultancyEngagement e = activeEngagement();
        e.setStatus(EngagementStatus.COMPLETED);
        e.setAgreedAmount(new BigDecimal("5000.00"));
        when(engagementRepository.findByIdForUpdate(30L)).thenReturn(Optional.of(e));
        when(invoiceRepository.findBySourceModuleAndSourceTypeAndSourceId(any(), any(), any()))
                .thenReturn(Optional.empty());
        var res = service.createEngagementInvoice(30L);
        assertNotNull(res.getInvoiceId());
    }

    private ConsultancyProposal sentProposal() {
        ConsultancyProposal p = new ConsultancyProposal();
        setId(p, 20L);
        p.setProposalNumber("PROP-1");
        p.setCustomer(client);
        p.setTitle("Biosecurity package");
        p.setProposedAmount(new BigDecimal("15000.00"));
        p.setCurrency("ETB");
        p.setStatus(ProposalStatus.SENT);
        return p;
    }

    private ConsultancyEngagement draftEngagement() {
        ConsultancyEngagement e = new ConsultancyEngagement();
        setId(e, 30L);
        e.setEngagementNumber("ENG-1");
        e.setCustomer(client);
        e.setTitle("Advisory engagement");
        e.setStatus(EngagementStatus.DRAFT);
        e.setAgreedAmount(new BigDecimal("10000.00"));
        e.setCurrency("ETB");
        e.setBillingType(BillingType.FIXED_PRICE);
        e.setInvoicedAmount(ZERO());
        e.setPaidAmount(ZERO());
        e.setOutstandingAmount(ZERO());
        e.setPaymentStatus(ConsultancyPaymentStatus.UNPAID);
        e.setLeadConsultant(user);
        e.setPriority(RequestPriority.MEDIUM);
        return e;
    }

    private ConsultancyEngagement activeEngagement() {
        ConsultancyEngagement e = draftEngagement();
        e.setStatus(EngagementStatus.ACTIVE);
        return e;
    }

    private void setAuth(String... authorities) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "consultant@test.com", "x",
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private FinanceTransaction tx(Long id) {
        FinanceTransaction t = new FinanceTransaction();
        setId(t, id);
        return t;
    }

    private <T> T withId(T entity) {
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
