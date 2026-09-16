package com.trustagro.crm.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.consultancy.dto.CrmQuotationConversionResult;
import com.trustagro.consultancy.repository.ConsultancyContractRepository;
import com.trustagro.consultancy.repository.ConsultancyEngagementRepository;
import com.trustagro.consultancy.service.ConsultancyService;
import com.trustagro.crm.dto.*;
import com.trustagro.crm.entity.*;
import com.trustagro.crm.repository.*;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.security.Permissions;
import com.trustagro.training.repository.TrainingRegistrationRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Backend-authority tests for CRM commercial workflow:
 * lead → opportunity → quotation → order, plus customer 360.
 */
@ExtendWith(MockitoExtension.class)
class CrmCommercialServiceTest {

    @Mock CrmLeadRepository leadRepository;
    @Mock CrmOpportunityRepository opportunityRepository;
    @Mock CrmQuotationRepository quotationRepository;
    @Mock CrmTaskRepository taskRepository;
    @Mock CrmActivityRepository activityRepository;
    @Mock CrmClientRepository clientRepository;
    @Mock CrmNumberSequenceRepository sequenceRepository;
    @Mock CustomerOrderRepository orderRepository;
    @Mock CrmOrderPaymentRepository paymentRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock ConsultancyEngagementRepository engagementRepository;
    @Mock ConsultancyContractRepository contractRepository;
    @Mock TrainingRegistrationRepository trainingRegistrationRepository;
    @Mock CustomerOrderService orderService;
    @Mock CrmService crmService;
    @Mock ConsultancyService consultancyService;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;

    @InjectMocks CrmCommercialService service;

    private final AtomicLong ids = new AtomicLong(1);
    private final Map<String, CrmNumberSequence> sequences = new HashMap<>();

    private User user;
    private CrmClient client;

    @BeforeEach
    void setUp() {
        setAuth(Permissions.CRM_READ, Permissions.CRM_CREATE, Permissions.CRM_UPDATE,
                Permissions.CRM_LEAD, Permissions.CRM_LEAD_CONVERT, Permissions.CRM_OPPORTUNITY,
                Permissions.CRM_QUOTATION, Permissions.CRM_QUOTATION_APPROVE,
                Permissions.CRM_ORDER, Permissions.CRM_ORDER_CONFIRM,
                Permissions.CRM_ORDER_INVOICE, Permissions.CRM_ORDER_PAY,
                Permissions.CRM_TASK, Permissions.CRM_REPORT);

        user = new User();
        setId(user, 1L);
        user.setEmail("crm@test.com");
        user.setFullName("CRM Manager");
        lenient().when(userRepository.findByEmail("crm@test.com")).thenReturn(Optional.of(user));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        client = new CrmClient();
        setId(client, 5L);
        client.setClientName("Farm Client");
        client.setCustomerCode("CUS-000001");
        client.setEmail("farm@example.com");
        client.setPhone("0911000000");
        client.setStatus(ClientStatus.ACTIVE);
        lenient().when(clientRepository.findById(5L)).thenReturn(Optional.of(client));

        lenient().when(sequenceRepository.findByNameForUpdate(anyString())).thenAnswer(inv ->
                Optional.of(sequences.computeIfAbsent(inv.getArgument(0), name -> {
                    CrmNumberSequence seq = new CrmNumberSequence();
                    seq.setName(name);
                    seq.setNextValue(1L);
                    return seq;
                })));
        lenient().when(sequenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sequenceRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        lenient().when(leadRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(opportunityRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(quotationRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(clientRepository.save(any())).thenAnswer(inv -> withId(inv.getArgument(0)));
        lenient().when(crmService.nextCustomerCode()).thenReturn("CUS-000099");
        lenient().when(crmService.toClientResponse(any())).thenAnswer(inv -> {
            CrmClient c = inv.getArgument(0);
            CrmClientResponse r = new CrmClientResponse();
            r.setId(c.getId());
            r.setClientName(c.getClientName());
            r.setCustomerCode(c.getCustomerCode());
            r.setEmail(c.getEmail());
            r.setPhone(c.getPhone());
            return r;
        });
        lenient().when(crmService.getVisitsByClient(anyLong())).thenReturn(List.of());
        lenient().when(orderService.toResponse(any())).thenAnswer(inv -> {
            CustomerOrder o = inv.getArgument(0);
            return OrderResponse.builder()
                    .id(o.getId())
                    .orderNumber(o.getOrderNumber())
                    .clientId(o.getClient() != null ? o.getClient().getId() : null)
                    .status(o.getStatus())
                    .totalAmount(o.getTotalAmount())
                    .build();
        });
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // ─── A. Lead create → qualify → convert ──────────────────────

    @Test
    void leadCreateQualifyConvertCreatesClientAndOpportunity_auditsAndNotifies() {
        LeadRequest createReq = new LeadRequest();
        createReq.setName("Abebe Kebede");
        createReq.setCompany("Abebe Farms");
        createReq.setEmail("abebe@farms.et");
        createReq.setPhone("0911223344");
        createReq.setSource(CrmLeadSource.REFERRAL);

        LeadResponse created = service.createLead(createReq);
        assertEquals(CrmLeadStatus.NEW, created.getStatus());
        assertTrue(created.getLeadNumber().startsWith("LEAD-"));
        verify(auditService).log(eq("CREATE"), eq("CRM"), eq("CrmLead"), any(), isNull(), anyString());
        verify(notificationService).createRoleNotification(eq("CRM lead created"), anyString(), eq(Role.CRM_MANAGER));

        CrmLead lead = new CrmLead();
        setId(lead, created.getId());
        lead.setLeadNumber(created.getLeadNumber());
        lead.setName("Abebe Kebede");
        lead.setCompany("Abebe Farms");
        lead.setEmail("abebe@farms.et");
        lead.setPhone("0911223344");
        lead.setStatus(CrmLeadStatus.NEW);
        lead.setSource(CrmLeadSource.REFERRAL);
        when(leadRepository.findById(created.getId())).thenReturn(Optional.of(lead));
        when(leadRepository.findByIdForUpdate(created.getId())).thenReturn(Optional.of(lead));
        when(clientRepository.findByEmailOrPhone("abebe@farms.et", "0911223344")).thenReturn(List.of());

        LeadStatusRequest qualify = new LeadStatusRequest();
        qualify.setStatus(CrmLeadStatus.QUALIFIED);
        // NEW → QUALIFIED allowed
        LeadResponse qualified = service.changeLeadStatus(created.getId(), qualify);
        assertEquals(CrmLeadStatus.QUALIFIED, qualified.getStatus());
        verify(auditService).log(eq("STATUS"), eq("CRM"), eq("CrmLead"), eq(created.getId()),
                eq("NEW"), eq("QUALIFIED"));

        LeadConvertRequest convert = new LeadConvertRequest();
        convert.setOpportunityTitle("Feed supply deal");
        LeadResponse converted = service.convertLead(created.getId(), convert);

        assertEquals(CrmLeadStatus.CONVERTED, converted.getStatus());
        assertNotNull(converted.getConvertedClientId());
        assertNotNull(converted.getConvertedOpportunityId());
        verify(clientRepository).save(any(CrmClient.class));
        verify(opportunityRepository).save(any(CrmOpportunity.class));
        verify(auditService).log(eq("CONVERT"), eq("CRM"), eq("CrmLead"), eq(created.getId()), anyString(), anyString());
        verify(notificationService).createRoleNotification(eq("CRM lead converted"), anyString(), eq(Role.CRM_MANAGER));
    }

    // ─── B. Duplicate email/phone without existingClientId ───────

    @Test
    void convertWithDuplicateEmailOrPhoneWithoutExistingClientIdThrows_noSilentMerge() {
        CrmLead lead = qualifiedLead(2L);
        lead.setEmail("dup@example.com");
        lead.setPhone("0999888777");
        when(leadRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(lead));

        CrmClient existing = new CrmClient();
        setId(existing, 88L);
        existing.setClientName("Existing Dup");
        existing.setEmail("dup@example.com");
        when(clientRepository.findByEmailOrPhone("dup@example.com", "0999888777"))
                .thenReturn(List.of(existing));

        LeadConvertRequest req = new LeadConvertRequest();
        BusinessException ex = assertThrows(BusinessException.class, () -> service.convertLead(2L, req));
        assertTrue(ex.getMessage().contains("Possible duplicate clients"));
        assertTrue(ex.getMessage().contains("88"));
        verify(clientRepository, never()).save(any());
        verify(opportunityRepository, never()).save(any());
    }

    // ─── C. Convert with existingClientId ────────────────────────

    @Test
    void convertWithExistingClientIdLinksExistingClient() {
        CrmLead lead = qualifiedLead(3L);
        when(leadRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(lead));

        LeadConvertRequest req = new LeadConvertRequest();
        req.setExistingClientId(5L);
        req.setOpportunityTitle("Linked opportunity");

        LeadResponse converted = service.convertLead(3L, req);
        assertEquals(CrmLeadStatus.CONVERTED, converted.getStatus());
        assertEquals(5L, converted.getConvertedClientId());
        verify(clientRepository, never()).save(any());
        ArgumentCaptor<CrmOpportunity> oppCap = ArgumentCaptor.forClass(CrmOpportunity.class);
        verify(opportunityRepository).save(oppCap.capture());
        assertEquals(5L, oppCap.getValue().getClient().getId());
        assertEquals("Linked opportunity", oppCap.getValue().getTitle());
    }

    // ─── D. Opportunity stage changes ────────────────────────────

    @Test
    void opportunityStageChanges_invalidRejected() {
        CrmOpportunity opp = new CrmOpportunity();
        setId(opp, 40L);
        opp.setOpportunityNumber("OPP-000001");
        opp.setClient(client);
        opp.setTitle("Pipeline");
        opp.setStage(OpportunityStage.PROSPECTING);
        when(opportunityRepository.findById(40L)).thenReturn(Optional.of(opp));

        OpportunityStageRequest ok = new OpportunityStageRequest();
        ok.setStage(OpportunityStage.QUALIFICATION);
        OpportunityResponse advanced = service.changeStage(40L, ok);
        assertEquals(OpportunityStage.QUALIFICATION, advanced.getStage());
        verify(auditService).log(eq("STAGE"), eq("CRM"), eq("CrmOpportunity"), eq(40L),
                eq("PROSPECTING"), eq("QUALIFICATION"));
        verify(notificationService).createRoleNotification(eq("Opportunity stage changed"), anyString(), eq(Role.CRM_MANAGER));

        OpportunityStageRequest bad = new OpportunityStageRequest();
        bad.setStage(OpportunityStage.WON); // QUALIFICATION → WON not allowed
        assertThrows(BusinessException.class, () -> service.changeStage(40L, bad));
        assertEquals(OpportunityStage.QUALIFICATION, opp.getStage());
    }

    // ─── E. Quotation create / totals / approve / send / accept ──

    @Test
    void quotationCreateRecalculatesTotals_approveSendAcceptCreatesOrder_duplicateAcceptIdempotent() {
        when(clientRepository.findById(5L)).thenReturn(Optional.of(client));

        QuotationRequest req = new QuotationRequest();
        req.setClientId(5L);
        req.setQuotationType(QuotationType.PRODUCT);
        QuotationItemRequest line = new QuotationItemRequest();
        line.setDescription("Broiler feed");
        line.setProductOrService("FEED-50KG");
        line.setQuantity(10.0);
        line.setUnitPrice(new BigDecimal("100.00"));
        line.setDiscountAmount(new BigDecimal("50.00"));
        line.setTaxAmount(new BigDecimal("20.00"));
        req.setItems(List.of(line));
        // header discount/tax left null → use line aggregates
        req.setDiscountAmount(null);
        req.setTaxAmount(null);

        QuotationResponse created = service.createQuotation(req);
        assertEquals(QuotationStatus.DRAFT, created.getStatus());
        assertEquals(0, new BigDecimal("1000.00").compareTo(created.getSubtotal()));
        assertEquals(0, new BigDecimal("50.00").compareTo(created.getDiscountAmount()));
        assertEquals(0, new BigDecimal("20.00").compareTo(created.getTaxAmount()));
        // total = 1000 - 50 + 20 = 970
        assertEquals(0, new BigDecimal("970.00").compareTo(created.getTotalAmount()));
        verify(auditService).log(eq("CREATE"), eq("CRM"), eq("CrmQuotation"), any(), isNull(), anyString());

        CrmQuotation q = quotationEntity(created.getId(), QuotationStatus.DRAFT, created.getTotalAmount());
        when(quotationRepository.findById(created.getId())).thenReturn(Optional.of(q));

        service.submitQuotation(created.getId());
        assertEquals(QuotationStatus.SUBMITTED, q.getStatus());

        service.approveQuotation(created.getId());
        assertEquals(QuotationStatus.APPROVED, q.getStatus());
        verify(auditService).log(eq("APPROVE"), eq("CRM"), eq("CrmQuotation"), eq(created.getId()),
                isNull(), eq("APPROVED"));

        service.sendQuotation(created.getId());
        assertEquals(QuotationStatus.SENT, q.getStatus());

        CustomerOrder order = new CustomerOrder();
        setId(order, 70L);
        order.setOrderNumber("ORD-000001");
        order.setClient(client);
        order.setStatus(CustomerOrderStatus.DRAFT);
        order.setTotalAmount(created.getTotalAmount());
        when(orderService.createFromQuotation(q)).thenReturn(order);

        QuotationResponse accepted = service.acceptQuotation(created.getId());
        assertEquals(QuotationStatus.ACCEPTED, accepted.getStatus());
        assertEquals(70L, accepted.getConvertedOrderId());
        verify(orderService, times(1)).createFromQuotation(q);
        verify(auditService).log(eq("ACCEPT"), eq("CRM"), eq("CrmQuotation"), eq(created.getId()), isNull(), contains("order=70"));
        verify(notificationService).createRoleNotification(eq("Quotation accepted"), anyString(), eq(Role.CRM_MANAGER));

        // Duplicate accept is idempotent
        QuotationResponse again = service.acceptQuotation(created.getId());
        assertEquals(70L, again.getConvertedOrderId());
        verify(orderService, times(1)).createFromQuotation(any());
    }

    // ─── F. Quotation reject / cancel / expiry validation ────────

    @Test
    void quotationRejectCancelAndExpiryValidation() {
        CrmQuotation submitted = quotationEntity(80L, QuotationStatus.SUBMITTED, new BigDecimal("100.00"));
        when(quotationRepository.findById(80L)).thenReturn(Optional.of(submitted));

        QuotationResponse rejected = service.rejectQuotation(80L);
        assertEquals(QuotationStatus.REJECTED, rejected.getStatus());

        CrmQuotation draft = quotationEntity(81L, QuotationStatus.DRAFT, new BigDecimal("100.00"));
        when(quotationRepository.findById(81L)).thenReturn(Optional.of(draft));
        QuotationResponse cancelled = service.cancelQuotation(81L);
        assertEquals(QuotationStatus.CANCELLED, cancelled.getStatus());

        // Cannot reject a DRAFT
        CrmQuotation stillDraft = quotationEntity(82L, QuotationStatus.DRAFT, new BigDecimal("100.00"));
        when(quotationRepository.findById(82L)).thenReturn(Optional.of(stillDraft));
        assertThrows(BusinessException.class, () -> service.rejectQuotation(82L));

        // Cannot accept an EXPIRED quotation
        CrmQuotation expired = quotationEntity(83L, QuotationStatus.EXPIRED, new BigDecimal("100.00"));
        when(quotationRepository.findById(83L)).thenReturn(Optional.of(expired));
        assertThrows(BusinessException.class, () -> service.acceptQuotation(83L));

        // Cannot accept a CANCELLED quotation
        CrmQuotation cancelledQ = quotationEntity(84L, QuotationStatus.CANCELLED, new BigDecimal("100.00"));
        when(quotationRepository.findById(84L)).thenReturn(Optional.of(cancelledQ));
        assertThrows(BusinessException.class, () -> service.acceptQuotation(84L));

        // Cannot go DRAFT → APPROVED (must submit first)
        CrmQuotation forApprove = quotationEntity(85L, QuotationStatus.DRAFT, new BigDecimal("100.00"));
        when(quotationRepository.findById(85L)).thenReturn(Optional.of(forApprove));
        assertThrows(BusinessException.class, () -> service.approveQuotation(85L));
    }

    // ─── J. Customer 360 ─────────────────────────────────────────

    @Test
    void customer360ReturnsRelationshipsWithoutInventingData() {
        when(clientRepository.findById(5L)).thenReturn(Optional.of(client));

        CrmLead convertedLead = qualifiedLead(90L);
        convertedLead.setStatus(CrmLeadStatus.CONVERTED);
        convertedLead.setConvertedClient(client);
        when(leadRepository.findByConvertedClientId(5L)).thenReturn(List.of(convertedLead));

        CrmOpportunity opp = new CrmOpportunity();
        setId(opp, 91L);
        opp.setOpportunityNumber("OPP-000091");
        opp.setClient(client);
        opp.setTitle("Existing opp");
        opp.setStage(OpportunityStage.PROPOSAL);
        when(opportunityRepository.findByClientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(opp));

        CrmQuotation q = quotationEntity(92L, QuotationStatus.SENT, new BigDecimal("300.00"));
        when(quotationRepository.findByClientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(q));

        CustomerOrder order = new CustomerOrder();
        setId(order, 93L);
        order.setOrderNumber("ORD-000093");
        order.setClient(client);
        order.setStatus(CustomerOrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("300.00"));
        when(orderRepository.findByClientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(order));
        when(orderRepository.findByClientId(5L)).thenReturn(List.of(order));

        when(taskRepository.findByClientIdOrderByDueDateAsc(5L)).thenReturn(List.of());
        when(activityRepository.findByClientIdOrderByActivityDateDesc(5L)).thenReturn(List.of());
        when(invoiceRepository.findByClientId(5L)).thenReturn(List.of());
        when(paymentRepository.findByOrderIdIn(List.of(93L))).thenReturn(List.of());
        when(engagementRepository.findByCustomerIdOrderByCreatedAtDesc(5L)).thenReturn(List.of());
        when(trainingRegistrationRepository.findByClientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of());

        Map<String, Object> view = service.customer360(5L);

        assertNotNull(view.get("client"));
        assertEquals(1, ((List<?>) view.get("leads")).size());
        assertEquals(1, ((List<?>) view.get("opportunities")).size());
        assertEquals(1, ((List<?>) view.get("quotations")).size());
        assertEquals(1, ((List<?>) view.get("orders")).size());
        assertTrue(((List<?>) view.get("visits")).isEmpty());
        assertTrue(((List<?>) view.get("tasks")).isEmpty());
        assertTrue(((List<?>) view.get("activities")).isEmpty());
        // Finance keys present because auth has CRM_ORDER_PAY — but empty, not invented
        assertTrue(view.containsKey("invoices"));
        assertTrue(((List<?>) view.get("invoices")).isEmpty());
        assertTrue(view.containsKey("payments"));
        assertTrue(((List<?>) view.get("payments")).isEmpty());
        verify(paymentRepository, times(1)).findByOrderIdIn(List.of(93L));
        verify(paymentRepository, never()).findByOrderIdOrderByCreatedAtDesc(anyLong());

        // Without finance authorities, invoices/payments must be omitted (not fabricated)
        setAuth(Permissions.CRM_READ);
        Map<String, Object> limited = service.customer360(5L);
        assertFalse(limited.containsKey("invoices"));
        assertFalse(limited.containsKey("payments"));
        assertEquals(1, ((List<?>) limited.get("orders")).size());
    }

    @Test
    void forceCreateBypassesDuplicateRejection() {
        CrmLead lead = qualifiedLead(4L);
        lead.setEmail("dup2@example.com");
        lead.setPhone("0911111111");
        when(leadRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(lead));
        CrmClient existing = new CrmClient();
        setId(existing, 77L);
        existing.setClientName("Existing");
        when(clientRepository.findByEmailOrPhone("dup2@example.com", "0911111111"))
                .thenReturn(List.of(existing));

        LeadConvertRequest req = new LeadConvertRequest();
        req.setForceCreate(true);
        LeadResponse converted = service.convertLead(4L, req);
        assertEquals(CrmLeadStatus.CONVERTED, converted.getStatus());
        verify(clientRepository).save(any(CrmClient.class));
    }

    @Test
    void reconvertAlreadyConvertedIsIdempotent() {
        CrmLead lead = qualifiedLead(5L);
        lead.setStatus(CrmLeadStatus.CONVERTED);
        lead.setConvertedClient(client);
        when(leadRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(lead));

        LeadResponse again = service.convertLead(5L, new LeadConvertRequest());
        assertEquals(CrmLeadStatus.CONVERTED, again.getStatus());
        verify(clientRepository, never()).save(any());
        verify(opportunityRepository, never()).save(any());
    }

    @Test
    void quotationIgnoresClientSuppliedTotals() {
        QuotationRequest req = new QuotationRequest();
        req.setClientId(5L);
        QuotationItemRequest line = new QuotationItemRequest();
        line.setDescription("Item");
        line.setQuantity(2.0);
        line.setUnitPrice(new BigDecimal("50.00"));
        req.setItems(List.of(line));
        // Fake client totals — server must recalculate from lines
        req.setDiscountAmount(BigDecimal.ZERO);
        req.setTaxAmount(BigDecimal.ZERO);

        QuotationResponse created = service.createQuotation(req);
        assertEquals(0, new BigDecimal("100.00").compareTo(created.getSubtotal()));
        assertEquals(0, new BigDecimal("100.00").compareTo(created.getTotalAmount()));
    }

    @Test
    void consultancyAcceptCreatesRequestEngagementContract_repeatedAcceptIdempotent() {
        CrmQuotation q = quotationEntity(200L, QuotationStatus.SENT, new BigDecimal("5000.00"));
        q.setQuotationType(QuotationType.CONSULTANCY);
        when(quotationRepository.findById(200L)).thenReturn(Optional.of(q));

        CrmQuotationConversionResult result = new CrmQuotationConversionResult(301L, 302L, 303L);
        when(consultancyService.convertFromCrmQuotation(200L)).thenReturn(result);

        QuotationResponse accepted = service.acceptQuotation(200L);
        assertEquals(QuotationStatus.ACCEPTED, accepted.getStatus());
        assertEquals(301L, accepted.getConsultancyRequestId());
        assertEquals(302L, accepted.getConsultancyEngagementId());
        assertEquals(303L, accepted.getConsultancyContractId());
        verify(consultancyService, times(1)).convertFromCrmQuotation(200L);
        verify(consultancyService, never()).createRequest(any());

        QuotationResponse again = service.acceptQuotation(200L);
        assertEquals(301L, again.getConsultancyRequestId());
        assertEquals(302L, again.getConsultancyEngagementId());
        assertEquals(303L, again.getConsultancyContractId());
        verify(consultancyService, times(1)).convertFromCrmQuotation(200L);
    }

    @Test
    void commercialDashboardReportsConversionRateAndCrmLinkedContracts() {
        when(leadRepository.countGroupedByStatus()).thenReturn(List.of(
                new Object[]{CrmLeadStatus.NEW, 2L},
                new Object[]{CrmLeadStatus.CONVERTED, 3L}
        ));
        when(opportunityRepository.countGroupedByStage()).thenReturn(List.of());
        when(opportunityRepository.sumOpenEstimatedValue()).thenReturn(BigDecimal.ZERO);
        when(quotationRepository.countGroupedByStatus()).thenReturn(List.of());
        when(orderRepository.countGroupedByStatus()).thenReturn(List.of());
        when(invoiceRepository.sumTotalAmountBySourceModule(anyString())).thenReturn(new BigDecimal("1000.00"));
        when(invoiceRepository.sumPaidAmountBySourceModule(anyString())).thenReturn(new BigDecimal("400.00"));
        when(invoiceRepository.sumOutstandingBySourceModule(anyString())).thenReturn(new BigDecimal("600.00"));
        when(invoiceRepository.countBySourceModule(anyString())).thenReturn(2L);
        when(taskRepository.countByStatus(CrmTaskStatus.TODO)).thenReturn(1L);
        when(taskRepository.countByStatus(CrmTaskStatus.IN_PROGRESS)).thenReturn(1L);
        when(leadRepository.count()).thenReturn(5L);
        when(leadRepository.countByStatus(CrmLeadStatus.CONVERTED)).thenReturn(3L);
        when(contractRepository.countByEngagementRequestCrmQuotationIdIsNotNull()).thenReturn(4L);

        Map<String, Object> dash = service.commercialDashboard();
        assertEquals(0, new BigDecimal("60.00").compareTo((BigDecimal) dash.get("conversionRate")));
        assertEquals(4L, dash.get("crmLinkedContracts"));
        assertEquals(0, new BigDecimal("600.00").compareTo((BigDecimal) dash.get("outstandingAr")));
        assertEquals(0, new BigDecimal("1000.00").compareTo((BigDecimal) dash.get("totalOrderRevenueInvoiced")));
        verify(leadRepository, never()).findAll();
        verify(orderRepository, never()).findAll();
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private CrmLead qualifiedLead(Long id) {
        CrmLead lead = new CrmLead();
        setId(lead, id);
        lead.setLeadNumber("LEAD-" + String.format("%06d", id));
        lead.setName("Lead " + id);
        lead.setCompany("Co " + id);
        lead.setEmail("lead" + id + "@test.et");
        lead.setPhone("0900000" + id);
        lead.setStatus(CrmLeadStatus.QUALIFIED);
        lead.setSource(CrmLeadSource.WEBSITE);
        lead.setPriority(CrmPriority.MEDIUM);
        return lead;
    }

    private CrmQuotation quotationEntity(Long id, QuotationStatus status, BigDecimal total) {
        CrmQuotation q = new CrmQuotation();
        setId(q, id);
        q.setQuotationNumber("QUO-" + String.format("%06d", id));
        q.setClient(client);
        q.setQuotationType(QuotationType.PRODUCT);
        q.setStatus(status);
        q.setCurrency("ETB");
        q.setIssueDate(LocalDate.now());
        q.setSubtotal(total);
        q.setDiscountAmount(ZERO());
        q.setTaxAmount(ZERO());
        q.setTotalAmount(total);
        q.setItems(new ArrayList<>());
        return q;
    }

    private void setAuth(String... authorities) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "crm@test.com", "x",
                java.util.Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList());
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
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
