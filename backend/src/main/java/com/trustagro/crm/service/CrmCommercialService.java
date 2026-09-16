package com.trustagro.crm.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.consultancy.dto.CrmQuotationConversionResult;
import com.trustagro.consultancy.repository.ConsultancyContractRepository;
import com.trustagro.consultancy.repository.ConsultancyEngagementRepository;
import com.trustagro.consultancy.service.ConsultancyService;
import com.trustagro.crm.dto.*;
import com.trustagro.crm.entity.*;
import com.trustagro.crm.repository.*;
import com.trustagro.finance.entity.FinanceSourceTypes;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.security.Permissions;
import com.trustagro.training.repository.TrainingRegistrationRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrmCommercialService {

    private static final String MODULE = "CRM";
    private static final String SEQ_LEAD = "LEAD";
    private static final String SEQ_OPP = "OPPORTUNITY";
    private static final String SEQ_QUO = "QUOTATION";
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final CrmLeadRepository leadRepository;
    private final CrmOpportunityRepository opportunityRepository;
    private final CrmQuotationRepository quotationRepository;
    private final CrmTaskRepository taskRepository;
    private final CrmActivityRepository activityRepository;
    private final CrmClientRepository clientRepository;
    private final CrmNumberSequenceRepository sequenceRepository;
    private final CustomerOrderRepository orderRepository;
    private final CrmOrderPaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final ConsultancyEngagementRepository engagementRepository;
    private final ConsultancyContractRepository contractRepository;
    private final TrainingRegistrationRepository trainingRegistrationRepository;
    private final CustomerOrderService orderService;
    private final CrmService crmService;
    private final ConsultancyService consultancyService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ─── Leads ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<LeadResponse> listLeads(CrmLeadStatus status) {
        List<CrmLead> list = status != null
                ? leadRepository.findByStatusOrderByCreatedAtDesc(status)
                : leadRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::toLeadResponse).toList();
    }

    @Transactional(readOnly = true)
    public LeadResponse getLead(Long id) {
        return toLeadResponse(requireLead(id));
    }

    @Transactional
    public LeadResponse createLead(LeadRequest req) {
        CrmLead lead = new CrmLead();
        lead.setLeadNumber(nextNumber(SEQ_LEAD, "LEAD"));
        mapLead(req, lead);
        lead.setStatus(CrmLeadStatus.NEW);
        CrmLead saved = leadRepository.save(lead);
        auditService.log("CREATE", MODULE, "CrmLead", saved.getId(), null, saved.getLeadNumber());
        notificationService.createRoleNotification(
                "CRM lead created",
                saved.getLeadNumber() + " — " + saved.getName(),
                Role.CRM_MANAGER);
        return toLeadResponse(saved);
    }

    @Transactional
    public LeadResponse updateLead(Long id, LeadRequest req) {
        CrmLead lead = requireLead(id);
        if (lead.getStatus() == CrmLeadStatus.CONVERTED || lead.getStatus() == CrmLeadStatus.LOST
                || lead.getStatus() == CrmLeadStatus.UNQUALIFIED) {
            throw new BusinessException("Cannot update lead in terminal status " + lead.getStatus());
        }
        mapLead(req, lead);
        return toLeadResponse(leadRepository.save(lead));
    }

    @Transactional
    public LeadResponse changeLeadStatus(Long id, LeadStatusRequest req) {
        CrmLead lead = requireLead(id);
        if (req.getStatus() == CrmLeadStatus.CONVERTED) {
            throw new BusinessException("Use convert endpoint to convert a lead");
        }
        LeadLifecycle.assertTransition(lead.getStatus(), req.getStatus());
        CrmLeadStatus prev = lead.getStatus();
        lead.setStatus(req.getStatus());
        CrmLead saved = leadRepository.save(lead);
        auditService.log("STATUS", MODULE, "CrmLead", saved.getId(), prev.name(), req.getStatus().name());
        return toLeadResponse(saved);
    }

    @Transactional
    public LeadResponse convertLead(Long id, LeadConvertRequest req) {
        CrmLead lead = leadRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
        if (lead.getStatus() == CrmLeadStatus.CONVERTED) {
            return toLeadResponse(lead);
        }
        if (lead.getStatus() != CrmLeadStatus.QUALIFIED && lead.getStatus() != CrmLeadStatus.CONTACTED
                && lead.getStatus() != CrmLeadStatus.NEW) {
            throw new BusinessException("Lead cannot be converted from status " + lead.getStatus());
        }
        if (lead.getStatus() != CrmLeadStatus.QUALIFIED) {
            // allow convert from NEW/CONTACTED via QUALIFIED hop
            if (lead.getStatus() == CrmLeadStatus.NEW) {
                LeadLifecycle.assertTransition(CrmLeadStatus.NEW, CrmLeadStatus.CONTACTED);
                lead.setStatus(CrmLeadStatus.CONTACTED);
            }
            if (lead.getStatus() == CrmLeadStatus.CONTACTED) {
                LeadLifecycle.assertTransition(CrmLeadStatus.CONTACTED, CrmLeadStatus.QUALIFIED);
                lead.setStatus(CrmLeadStatus.QUALIFIED);
            }
        }
        LeadLifecycle.assertTransition(CrmLeadStatus.QUALIFIED, CrmLeadStatus.CONVERTED);

        CrmClient client;
        if (req.getExistingClientId() != null) {
            client = clientRepository.findById(req.getExistingClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + req.getExistingClientId()));
        } else {
            List<CrmClient> candidates = findDuplicateClients(lead.getEmail(), lead.getPhone());
            if (!candidates.isEmpty() && !req.isForceCreate() && !req.isConfirmDuplicate()) {
                String ids = candidates.stream()
                        .map(c -> c.getId() + ":" + c.getClientName())
                        .collect(Collectors.joining(", "));
                throw new BusinessException(
                        "Possible duplicate clients found [" + ids
                                + "]. Provide existingClientId, or set forceCreate/confirmDuplicate.");
            }
            client = new CrmClient();
            client.setCustomerCode(crmService.nextCustomerCode());
            client.setClientName(lead.getCompany() != null && !lead.getCompany().isBlank()
                    ? lead.getCompany() : lead.getName());
            client.setContactPerson(lead.getContactPerson() != null ? lead.getContactPerson() : lead.getName());
            client.setPhone(lead.getPhone());
            client.setEmail(lead.getEmail());
            client.setNotes(lead.getNotes());
            client.setStatus(ClientStatus.ACTIVE);
            client.setAssignedOwner(lead.getAssignedOwner());
            client = clientRepository.save(client);
        }

        CrmOpportunity opp = new CrmOpportunity();
        opp.setOpportunityNumber(nextNumber(SEQ_OPP, "OPP"));
        opp.setClient(client);
        opp.setTitle(req.getOpportunityTitle() != null && !req.getOpportunityTitle().isBlank()
                ? req.getOpportunityTitle()
                : "Opportunity from " + lead.getLeadNumber());
        opp.setStage(OpportunityStage.PROSPECTING);
        opp.setSource(lead.getSource());
        opp.setPriority(lead.getPriority());
        opp.setLead(lead);
        opp.setSalesOwner(lead.getAssignedOwner());
        opp.setNotes(lead.getNotes());
        opp = opportunityRepository.save(opp);

        lead.setConvertedClient(client);
        lead.setConvertedOpportunity(opp);
        lead.setConvertedAt(LocalDateTime.now());
        lead.setConvertedBy(currentUser());
        lead.setStatus(CrmLeadStatus.CONVERTED);
        CrmLead saved = leadRepository.save(lead);

        auditService.log("CONVERT", MODULE, "CrmLead", saved.getId(),
                lead.getLeadNumber(), "client=" + client.getId() + ",opp=" + opp.getId());
        notificationService.createRoleNotification(
                "CRM lead converted",
                saved.getLeadNumber() + " → " + client.getClientName(),
                Role.CRM_MANAGER);
        return toLeadResponse(saved);
    }

    // ─── Opportunities ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<OpportunityResponse> listOpportunities(OpportunityStage stage) {
        List<CrmOpportunity> list = stage != null
                ? opportunityRepository.findByStageOrderByCreatedAtDesc(stage)
                : opportunityRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::toOpportunityResponse).toList();
    }

    @Transactional(readOnly = true)
    public OpportunityResponse getOpportunity(Long id) {
        return toOpportunityResponse(requireOpportunity(id));
    }

    @Transactional
    public OpportunityResponse createOpportunity(OpportunityRequest req) {
        CrmClient client = clientRepository.findById(req.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + req.getClientId()));
        CrmOpportunity opp = new CrmOpportunity();
        opp.setOpportunityNumber(nextNumber(SEQ_OPP, "OPP"));
        opp.setClient(client);
        mapOpportunity(req, opp);
        opp.setStage(OpportunityStage.PROSPECTING);
        CrmOpportunity saved = opportunityRepository.save(opp);
        auditService.log("CREATE", MODULE, "CrmOpportunity", saved.getId(), null, saved.getOpportunityNumber());
        return toOpportunityResponse(saved);
    }

    @Transactional
    public OpportunityResponse updateOpportunity(Long id, OpportunityRequest req) {
        CrmOpportunity opp = requireOpportunity(id);
        if (opp.getStage() == OpportunityStage.WON || opp.getStage() == OpportunityStage.LOST) {
            throw new BusinessException("Cannot update opportunity in terminal stage " + opp.getStage());
        }
        if (req.getClientId() != null && !req.getClientId().equals(opp.getClient().getId())) {
            CrmClient client = clientRepository.findById(req.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + req.getClientId()));
            opp.setClient(client);
        }
        mapOpportunity(req, opp);
        return toOpportunityResponse(opportunityRepository.save(opp));
    }

    @Transactional
    public OpportunityResponse changeStage(Long id, OpportunityStageRequest req) {
        CrmOpportunity opp = requireOpportunity(id);
        OpportunityLifecycle.assertTransition(opp.getStage(), req.getStage());
        OpportunityStage prev = opp.getStage();
        opp.setStage(req.getStage());
        if (req.getStage() == OpportunityStage.LOST) {
            opp.setLossReason(req.getLossReason());
        }
        if (req.getStage() == OpportunityStage.WON) {
            opp.setProbability(100);
        }
        CrmOpportunity saved = opportunityRepository.save(opp);
        auditService.log("STAGE", MODULE, "CrmOpportunity", saved.getId(), prev.name(), req.getStage().name());
        notificationService.createRoleNotification(
                "Opportunity stage changed",
                saved.getOpportunityNumber() + " → " + req.getStage(),
                Role.CRM_MANAGER);
        return toOpportunityResponse(saved);
    }

    // ─── Quotations ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<QuotationResponse> listQuotations(QuotationStatus status) {
        List<CrmQuotation> list = status != null
                ? quotationRepository.findByStatusOrderByCreatedAtDesc(status)
                : quotationRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::toQuotationResponse).toList();
    }

    @Transactional(readOnly = true)
    public QuotationResponse getQuotation(Long id) {
        return toQuotationResponse(requireQuotation(id));
    }

    @Transactional
    public QuotationResponse createQuotation(QuotationRequest req) {
        CrmClient client = clientRepository.findById(req.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + req.getClientId()));
        CrmQuotation q = new CrmQuotation();
        q.setQuotationNumber(nextNumber(SEQ_QUO, "QUO"));
        q.setClient(client);
        q.setQuotationType(req.getQuotationType() != null ? req.getQuotationType() : QuotationType.PRODUCT);
        q.setIssueDate(req.getIssueDate() != null ? req.getIssueDate() : LocalDate.now());
        q.setExpiryDate(req.getExpiryDate());
        q.setCurrency(req.getCurrency() != null ? req.getCurrency() : "ETB");
        q.setDiscountAmount(nvl(req.getDiscountAmount()));
        q.setTaxAmount(nvl(req.getTaxAmount()));
        q.setNotes(req.getNotes());
        q.setStatus(QuotationStatus.DRAFT);
        q.setCreatedBy(currentUser());
        if (req.getOpportunityId() != null) {
            opportunityRepository.findById(req.getOpportunityId()).ifPresent(q::setOpportunity);
        }
        applyQuotationItems(q, req.getItems());
        recalculateQuotation(q);
        CrmQuotation saved = quotationRepository.save(q);
        auditService.log("CREATE", MODULE, "CrmQuotation", saved.getId(), null, saved.getQuotationNumber());
        return toQuotationResponse(saved);
    }

    @Transactional
    public QuotationResponse updateQuotation(Long id, QuotationRequest req) {
        CrmQuotation q = requireQuotation(id);
        if (q.getStatus() != QuotationStatus.DRAFT) {
            throw new BusinessException("Only DRAFT quotations can be updated");
        }
        q.setIssueDate(req.getIssueDate() != null ? req.getIssueDate() : q.getIssueDate());
        q.setExpiryDate(req.getExpiryDate());
        if (req.getCurrency() != null) q.setCurrency(req.getCurrency());
        if (req.getQuotationType() != null) q.setQuotationType(req.getQuotationType());
        q.setDiscountAmount(nvl(req.getDiscountAmount()));
        q.setTaxAmount(nvl(req.getTaxAmount()));
        q.setNotes(req.getNotes());
        q.getItems().clear();
        applyQuotationItems(q, req.getItems());
        recalculateQuotation(q);
        return toQuotationResponse(quotationRepository.save(q));
    }

    @Transactional
    public QuotationResponse submitQuotation(Long id) {
        return transitionQuotation(id, QuotationStatus.SUBMITTED);
    }

    @Transactional
    public QuotationResponse approveQuotation(Long id) {
        CrmQuotation q = requireQuotation(id);
        QuotationLifecycle.assertTransition(q.getStatus(), QuotationStatus.APPROVED);
        User me = currentUser();
        if (me != null && q.getCreatedBy() != null && q.getCreatedBy().getId().equals(me.getId())) {
            throw new BusinessException("Cannot self-approve a quotation");
        }
        q.setStatus(QuotationStatus.APPROVED);
        q.setApprovedBy(me);
        CrmQuotation saved = quotationRepository.save(q);
        auditService.log("APPROVE", MODULE, "CrmQuotation", saved.getId(), null, QuotationStatus.APPROVED.name());
        return toQuotationResponse(saved);
    }

    @Transactional
    public QuotationResponse sendQuotation(Long id) {
        return transitionQuotation(id, QuotationStatus.SENT);
    }

    @Transactional
    public QuotationResponse acceptQuotation(Long id) {
        CrmQuotation q = requireQuotation(id);

        if (q.getStatus() == QuotationStatus.ACCEPTED) {
            if (q.getQuotationType() == QuotationType.CONSULTANCY
                    && q.getConsultancyRequestId() != null
                    && q.getConsultancyEngagementId() != null
                    && q.getConsultancyContractId() != null) {
                return toQuotationResponse(q);
            }
            if (q.getQuotationType() != QuotationType.CONSULTANCY && q.getConvertedOrderId() != null) {
                return toQuotationResponse(q);
            }
        }

        boolean firstAccept = q.getStatus() != QuotationStatus.ACCEPTED;
        if (firstAccept) {
            QuotationLifecycle.assertTransition(q.getStatus(), QuotationStatus.ACCEPTED);
            q.setStatus(QuotationStatus.ACCEPTED);
        }

        if (q.getQuotationType() == QuotationType.CONSULTANCY) {
            CrmQuotationConversionResult converted = consultancyService.convertFromCrmQuotation(q.getId());
            q.setConsultancyRequestId(converted.getRequestId());
            q.setConsultancyEngagementId(converted.getEngagementId());
            q.setConsultancyContractId(converted.getContractId());
        } else if (q.getConvertedOrderId() == null) {
            CustomerOrder order = orderService.createFromQuotation(q);
            q.setConvertedOrderId(order.getId());
        }

        if (firstAccept && q.getOpportunity() != null
                && q.getOpportunity().getStage() != OpportunityStage.WON
                && q.getOpportunity().getStage() != OpportunityStage.LOST) {
            try {
                OpportunityLifecycle.assertTransition(q.getOpportunity().getStage(), OpportunityStage.WON);
                q.getOpportunity().setStage(OpportunityStage.WON);
                q.getOpportunity().setProbability(100);
                opportunityRepository.save(q.getOpportunity());
            } catch (BusinessException ignored) {
                // stage may not allow WON from current; leave as-is
            }
        }

        CrmQuotation saved = quotationRepository.save(q);
        if (firstAccept) {
            auditService.log("ACCEPT", MODULE, "CrmQuotation", saved.getId(), null,
                    saved.getConvertedOrderId() != null
                            ? "order=" + saved.getConvertedOrderId()
                            : "consultancyRequest=" + saved.getConsultancyRequestId()
                            + ",engagement=" + saved.getConsultancyEngagementId()
                            + ",contract=" + saved.getConsultancyContractId());
            notificationService.createRoleNotification(
                    "Quotation accepted",
                    saved.getQuotationNumber() + " accepted",
                    Role.CRM_MANAGER);
        }
        return toQuotationResponse(saved);
    }

    @Transactional
    public QuotationResponse rejectQuotation(Long id) {
        return transitionQuotation(id, QuotationStatus.REJECTED);
    }

    @Transactional
    public QuotationResponse cancelQuotation(Long id) {
        return transitionQuotation(id, QuotationStatus.CANCELLED);
    }

    private QuotationResponse transitionQuotation(Long id, QuotationStatus next) {
        CrmQuotation q = requireQuotation(id);
        QuotationLifecycle.assertTransition(q.getStatus(), next);
        QuotationStatus prev = q.getStatus();
        q.setStatus(next);
        CrmQuotation saved = quotationRepository.save(q);
        auditService.log("STATUS", MODULE, "CrmQuotation", saved.getId(), prev.name(), next.name());
        return toQuotationResponse(saved);
    }

    // ─── Tasks / Activities ───────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(CrmTaskStatus status) {
        List<CrmTask> list = status != null
                ? taskRepository.findByStatusOrderByDueDateAsc(status)
                : taskRepository.findAllByOrderByDueDateAsc();
        return list.stream().map(this::toTaskResponse).toList();
    }

    @Transactional
    public TaskResponse createTask(TaskRequest req) {
        CrmTask task = new CrmTask();
        mapTask(req, task);
        task.setStatus(req.getStatus() != null ? req.getStatus() : CrmTaskStatus.TODO);
        CrmTask saved = taskRepository.save(task);
        auditService.log("CREATE", MODULE, "CrmTask", saved.getId(), null, saved.getTitle());
        return toTaskResponse(saved);
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest req) {
        CrmTask task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
        mapTask(req, task);
        if (req.getStatus() != null) {
            task.setStatus(req.getStatus());
            if (req.getStatus() == CrmTaskStatus.COMPLETED && task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
        }
        return toTaskResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> listActivities(Long clientId) {
        List<CrmActivity> list = clientId != null
                ? activityRepository.findByClientIdOrderByActivityDateDesc(clientId)
                : activityRepository.findAllByOrderByActivityDateDesc();
        return list.stream().map(this::toActivityResponse).toList();
    }

    @Transactional
    public ActivityResponse createActivity(ActivityRequest req) {
        CrmActivity a = new CrmActivity();
        a.setType(req.getType());
        a.setSubject(req.getSubject());
        a.setBody(req.getBody());
        a.setActivityDate(req.getActivityDate() != null ? req.getActivityDate() : LocalDateTime.now());
        a.setPerformedBy(currentUser());
        if (req.getClientId() != null) {
            clientRepository.findById(req.getClientId()).ifPresent(a::setClient);
        }
        if (req.getLeadId() != null) {
            leadRepository.findById(req.getLeadId()).ifPresent(a::setLead);
        }
        if (req.getOpportunityId() != null) {
            opportunityRepository.findById(req.getOpportunityId()).ifPresent(a::setOpportunity);
        }
        CrmActivity saved = activityRepository.save(a);
        auditService.log("CREATE", MODULE, "CrmActivity", saved.getId(), null, saved.getSubject());
        return toActivityResponse(saved);
    }

    // ─── Customer 360 / Dashboard ────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> customer360(Long clientId) {
        CrmClient client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("client", crmService.toClientResponse(client));
        map.put("leads", leadRepository.findByConvertedClientId(clientId).stream().map(this::toLeadResponse).toList());
        map.put("opportunities", opportunityRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream().map(this::toOpportunityResponse).toList());
        map.put("quotations", quotationRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream().map(this::toQuotationResponse).toList());
        map.put("orders", orderRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream().map(orderService::toResponse).toList());
        map.put("visits", crmService.getVisitsByClient(clientId));
        map.put("tasks", taskRepository.findByClientIdOrderByDueDateAsc(clientId)
                .stream().map(this::toTaskResponse).toList());
        map.put("activities", activityRepository.findByClientIdOrderByActivityDateDesc(clientId)
                .stream().map(this::toActivityResponse).toList());

        boolean canSeeFinance = hasAuthority(Permissions.CRM_ORDER_INVOICE)
                || hasAuthority(Permissions.CRM_ORDER_PAY)
                || hasAuthority(Permissions.CRM_REPORT)
                || hasAuthority(Permissions.FINANCE_READ);
        if (canSeeFinance) {
            map.put("invoices", invoiceRepository.findByClientId(clientId));
            List<CustomerOrder> clientOrders = orderRepository.findByClientId(clientId);
            List<Long> orderIds = clientOrders.stream().map(CustomerOrder::getId).toList();
            List<CrmOrderPayment> allPayments = orderIds.isEmpty()
                    ? List.of()
                    : paymentRepository.findByOrderIdIn(orderIds);
            List<OrderPaymentResponse> payments = allPayments.stream()
                    .map(p -> OrderPaymentResponse.builder()
                            .id(p.getId())
                            .paymentNumber(p.getPaymentNumber())
                            .orderId(p.getOrder() != null ? p.getOrder().getId() : null)
                            .invoiceId(p.getInvoice() != null ? p.getInvoice().getId() : null)
                            .amount(p.getAmount())
                            .method(p.getMethod())
                            .paymentDate(p.getPaymentDate())
                            .financeTransactionId(p.getFinanceTransactionId())
                            .createdAt(p.getCreatedAt())
                            .build())
                    .toList();
            map.put("payments", payments);
        }

        if (hasAuthority(Permissions.CONSULTANCY_READ) || hasAuthority(Permissions.CRM_READ)) {
            map.put("consultancyEngagements",
                    engagementRepository.findByCustomerIdOrderByCreatedAtDesc(clientId).stream()
                            .map(e -> Map.of(
                                    "id", e.getId(),
                                    "engagementNumber", e.getEngagementNumber() != null ? e.getEngagementNumber() : "",
                                    "title", e.getTitle() != null ? e.getTitle() : "",
                                    "status", e.getStatus() != null ? e.getStatus().name() : ""))
                            .toList());
        }
        if (hasAuthority(Permissions.TRAINING_READ) || hasAuthority(Permissions.CRM_READ)) {
            map.put("trainingRegistrations",
                    trainingRegistrationRepository.findByClientIdOrderByCreatedAtDesc(clientId).stream()
                            .map(r -> Map.of(
                                    "id", r.getId(),
                                    "status", r.getStatus() != null ? r.getStatus().name() : "",
                                    "sessionId", r.getSession() != null ? r.getSession().getId() : 0L))
                            .toList());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> commercialDashboard() {
        Map<String, Object> stats = new LinkedHashMap<>();

        Map<String, Long> leadsByStatus = new LinkedHashMap<>();
        for (Object[] row : leadRepository.countGroupedByStatus()) {
            leadsByStatus.put(((CrmLeadStatus) row[0]).name(), (Long) row[1]);
        }
        stats.put("pipelineLeads", leadsByStatus);

        Map<String, Long> oppByStage = new LinkedHashMap<>();
        for (Object[] row : opportunityRepository.countGroupedByStage()) {
            oppByStage.put(((OpportunityStage) row[0]).name(), (Long) row[1]);
        }
        stats.put("opportunitiesByStage", oppByStage);

        BigDecimal openOppValue = opportunityRepository.sumOpenEstimatedValue();
        stats.put("openOpportunityValue", openOppValue != null ? openOppValue : ZERO);

        Map<String, Long> quotesByStatus = new LinkedHashMap<>();
        for (Object[] row : quotationRepository.countGroupedByStatus()) {
            quotesByStatus.put(((QuotationStatus) row[0]).name(), (Long) row[1]);
        }
        stats.put("quotationsByStatus", quotesByStatus);

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (Object[] row : orderRepository.countGroupedByStatus()) {
            CustomerOrderStatus st = (CustomerOrderStatus) row[0];
            ordersByStatus.put(st != null ? st.name() : "UNKNOWN", (Long) row[1]);
        }
        stats.put("ordersByStatus", ordersByStatus);

        // Finance CRM invoices: SUM(totalAmount), SUM(paidAmount), outstanding AR
        BigDecimal invoiced = nvl(invoiceRepository.sumTotalAmountBySourceModule(FinanceSourceTypes.MODULE_CRM));
        BigDecimal paid = nvl(invoiceRepository.sumPaidAmountBySourceModule(FinanceSourceTypes.MODULE_CRM));
        BigDecimal outstandingAr = nvl(invoiceRepository.sumOutstandingBySourceModule(FinanceSourceTypes.MODULE_CRM));
        stats.put("totalOrderRevenueInvoiced", invoiced);
        stats.put("totalOrderPaid", paid);
        stats.put("outstandingAr", outstandingAr);

        stats.put("openTasks",
                taskRepository.countByStatus(CrmTaskStatus.TODO)
                        + taskRepository.countByStatus(CrmTaskStatus.IN_PROGRESS));

        stats.put("crmInvoices", invoiceRepository.countBySourceModule(FinanceSourceTypes.MODULE_CRM));

        // Formula: conversionRate = convertedLeads / totalLeads * 100 (zero if totalLeads == 0)
        long totalLeads = leadRepository.count();
        long convertedLeads = leadRepository.countByStatus(CrmLeadStatus.CONVERTED);
        BigDecimal conversionRate = totalLeads == 0
                ? ZERO
                : BigDecimal.valueOf(convertedLeads)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalLeads), 2, RoundingMode.HALF_UP);
        stats.put("conversionRate", conversionRate);
        stats.put("leadConversionRate", conversionRate);

        stats.put("crmLinkedContracts", contractRepository.countByEngagementRequestCrmQuotationIdIsNotNull());
        return stats;
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private void mapLead(LeadRequest req, CrmLead lead) {
        lead.setName(req.getName());
        lead.setCompany(req.getCompany());
        lead.setContactPerson(req.getContactPerson());
        lead.setPhone(req.getPhone());
        lead.setEmail(req.getEmail());
        if (req.getSource() != null) lead.setSource(req.getSource());
        lead.setIndustry(req.getIndustry());
        lead.setNotes(req.getNotes());
        if (req.getPriority() != null) lead.setPriority(req.getPriority());
        if (req.getMarketingLeadId() != null) lead.setMarketingLeadId(req.getMarketingLeadId());
        if (req.getAssignedOwnerId() != null) {
            userRepository.findById(req.getAssignedOwnerId()).ifPresent(lead::setAssignedOwner);
        }
    }

    private void mapOpportunity(OpportunityRequest req, CrmOpportunity opp) {
        opp.setTitle(req.getTitle());
        opp.setDescription(req.getDescription());
        if (req.getEstimatedValue() != null) opp.setEstimatedValue(nvl(req.getEstimatedValue()));
        if (req.getCurrency() != null) opp.setCurrency(req.getCurrency());
        if (req.getProbability() != null) {
            int p = Math.max(0, Math.min(100, req.getProbability()));
            opp.setProbability(p);
        }
        opp.setExpectedCloseDate(req.getExpectedCloseDate());
        if (req.getSource() != null) opp.setSource(req.getSource());
        if (req.getPriority() != null) opp.setPriority(req.getPriority());
        opp.setNotes(req.getNotes());
        if (req.getSalesOwnerId() != null) {
            userRepository.findById(req.getSalesOwnerId()).ifPresent(opp::setSalesOwner);
        }
        if (req.getLeadId() != null) {
            leadRepository.findById(req.getLeadId()).ifPresent(opp::setLead);
        }
    }

    private void mapTask(TaskRequest req, CrmTask task) {
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        task.setDueDate(req.getDueDate());
        if (req.getPriority() != null) task.setPriority(req.getPriority());
        if (req.getClientId() != null) {
            clientRepository.findById(req.getClientId()).ifPresent(task::setClient);
        }
        if (req.getLeadId() != null) {
            leadRepository.findById(req.getLeadId()).ifPresent(task::setLead);
        }
        if (req.getOpportunityId() != null) {
            opportunityRepository.findById(req.getOpportunityId()).ifPresent(task::setOpportunity);
        }
        if (req.getAssignedUserId() != null) {
            userRepository.findById(req.getAssignedUserId()).ifPresent(task::setAssignedUser);
        }
    }

    private void applyQuotationItems(CrmQuotation q, List<QuotationItemRequest> items) {
        if (items == null) return;
        for (QuotationItemRequest ir : items) {
            CrmQuotationItem item = new CrmQuotationItem();
            item.setDescription(ir.getDescription());
            item.setProductOrService(ir.getProductOrService());
            item.setQuantity(ir.getQuantity() != null ? ir.getQuantity() : 1.0);
            item.setUnit(ir.getUnit());
            item.setUnitPrice(nvl(ir.getUnitPrice()));
            item.setDiscountAmount(nvl(ir.getDiscountAmount()));
            item.setTaxAmount(nvl(ir.getTaxAmount()));
            BigDecimal line = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .subtract(item.getDiscountAmount())
                    .add(item.getTaxAmount())
                    .setScale(2, RoundingMode.HALF_UP);
            item.setLineTotal(line.max(ZERO));
            q.addItem(item);
        }
    }

    private void recalculateQuotation(CrmQuotation q) {
        BigDecimal subtotal = ZERO;
        BigDecimal lineTax = ZERO;
        BigDecimal lineDiscount = ZERO;
        for (CrmQuotationItem item : q.getItems()) {
            subtotal = subtotal.add(nvl(item.getUnitPrice())
                    .multiply(BigDecimal.valueOf(item.getQuantity() != null ? item.getQuantity() : 0)));
            lineTax = lineTax.add(nvl(item.getTaxAmount()));
            lineDiscount = lineDiscount.add(nvl(item.getDiscountAmount()));
        }
        q.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        BigDecimal headerDiscount = nvl(q.getDiscountAmount()).compareTo(ZERO) > 0
                ? nvl(q.getDiscountAmount()) : lineDiscount;
        BigDecimal headerTax = nvl(q.getTaxAmount()).compareTo(ZERO) > 0
                ? nvl(q.getTaxAmount()) : lineTax;
        q.setDiscountAmount(headerDiscount);
        q.setTaxAmount(headerTax);
        q.setTotalAmount(subtotal.subtract(headerDiscount).add(headerTax).max(ZERO));
    }

    private List<CrmClient> findDuplicateClients(String email, String phone) {
        String e = email != null && !email.isBlank() ? email.trim() : null;
        String p = phone != null && !phone.isBlank() ? phone.trim() : null;
        if (e == null && p == null) return List.of();
        return clientRepository.findByEmailOrPhone(e, p);
    }

    private String nextNumber(String sequenceName, String prefix) {
        CrmNumberSequence seq = sequenceRepository.findByNameForUpdate(sequenceName)
                .orElseGet(() -> {
                    CrmNumberSequence fresh = new CrmNumberSequence();
                    fresh.setName(sequenceName);
                    fresh.setNextValue(1L);
                    return sequenceRepository.saveAndFlush(fresh);
                });
        long value = seq.getNextValue();
        seq.setNextValue(value + 1);
        sequenceRepository.save(seq);
        return String.format("%s-%06d", prefix, value);
    }

    private CrmLead requireLead(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
    }

    private CrmOpportunity requireOpportunity(Long id) {
        return opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + id));
    }

    private CrmQuotation requireQuotation(Long id) {
        return quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found: " + id));
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    private boolean hasAuthority(String perm) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (perm.equals(ga.getAuthority())) return true;
        }
        return false;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private LeadResponse toLeadResponse(CrmLead l) {
        return LeadResponse.builder()
                .id(l.getId())
                .leadNumber(l.getLeadNumber())
                .name(l.getName())
                .company(l.getCompany())
                .contactPerson(l.getContactPerson())
                .phone(l.getPhone())
                .email(l.getEmail())
                .source(l.getSource())
                .industry(l.getIndustry())
                .notes(l.getNotes())
                .assignedOwnerId(l.getAssignedOwner() != null ? l.getAssignedOwner().getId() : null)
                .assignedOwnerName(l.getAssignedOwner() != null ? l.getAssignedOwner().getFullName() : null)
                .status(l.getStatus())
                .priority(l.getPriority())
                .convertedAt(l.getConvertedAt())
                .convertedClientId(l.getConvertedClient() != null ? l.getConvertedClient().getId() : null)
                .convertedOpportunityId(l.getConvertedOpportunity() != null ? l.getConvertedOpportunity().getId() : null)
                .convertedById(l.getConvertedBy() != null ? l.getConvertedBy().getId() : null)
                .marketingLeadId(l.getMarketingLeadId())
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    private OpportunityResponse toOpportunityResponse(CrmOpportunity o) {
        return OpportunityResponse.builder()
                .id(o.getId())
                .opportunityNumber(o.getOpportunityNumber())
                .clientId(o.getClient().getId())
                .clientName(o.getClient().getClientName())
                .title(o.getTitle())
                .description(o.getDescription())
                .estimatedValue(o.getEstimatedValue())
                .currency(o.getCurrency())
                .probability(o.getProbability())
                .expectedCloseDate(o.getExpectedCloseDate())
                .salesOwnerId(o.getSalesOwner() != null ? o.getSalesOwner().getId() : null)
                .salesOwnerName(o.getSalesOwner() != null ? o.getSalesOwner().getFullName() : null)
                .source(o.getSource())
                .stage(o.getStage())
                .priority(o.getPriority())
                .lossReason(o.getLossReason())
                .notes(o.getNotes())
                .leadId(o.getLead() != null ? o.getLead().getId() : null)
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    private QuotationResponse toQuotationResponse(CrmQuotation q) {
        List<QuotationItemResponse> items = q.getItems() == null ? List.of() : q.getItems().stream()
                .map(i -> QuotationItemResponse.builder()
                        .id(i.getId())
                        .description(i.getDescription())
                        .productOrService(i.getProductOrService())
                        .quantity(i.getQuantity())
                        .unit(i.getUnit())
                        .unitPrice(i.getUnitPrice())
                        .discountAmount(i.getDiscountAmount())
                        .taxAmount(i.getTaxAmount())
                        .lineTotal(i.getLineTotal())
                        .build())
                .toList();
        return QuotationResponse.builder()
                .id(q.getId())
                .quotationNumber(q.getQuotationNumber())
                .clientId(q.getClient().getId())
                .clientName(q.getClient().getClientName())
                .opportunityId(q.getOpportunity() != null ? q.getOpportunity().getId() : null)
                .quotationType(q.getQuotationType())
                .issueDate(q.getIssueDate())
                .expiryDate(q.getExpiryDate())
                .status(q.getStatus())
                .currency(q.getCurrency())
                .subtotal(q.getSubtotal())
                .discountAmount(q.getDiscountAmount())
                .taxAmount(q.getTaxAmount())
                .totalAmount(q.getTotalAmount())
                .notes(q.getNotes())
                .createdById(q.getCreatedBy() != null ? q.getCreatedBy().getId() : null)
                .approvedById(q.getApprovedBy() != null ? q.getApprovedBy().getId() : null)
                .convertedOrderId(q.getConvertedOrderId())
                .consultancyRequestId(q.getConsultancyRequestId())
                .consultancyEngagementId(q.getConsultancyEngagementId())
                .consultancyContractId(q.getConsultancyContractId())
                .items(items)
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }

    private TaskResponse toTaskResponse(CrmTask t) {
        return TaskResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .clientId(t.getClient() != null ? t.getClient().getId() : null)
                .leadId(t.getLead() != null ? t.getLead().getId() : null)
                .opportunityId(t.getOpportunity() != null ? t.getOpportunity().getId() : null)
                .assignedUserId(t.getAssignedUser() != null ? t.getAssignedUser().getId() : null)
                .assignedUserName(t.getAssignedUser() != null ? t.getAssignedUser().getFullName() : null)
                .dueDate(t.getDueDate())
                .priority(t.getPriority())
                .status(t.getStatus())
                .completedAt(t.getCompletedAt())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private ActivityResponse toActivityResponse(CrmActivity a) {
        return ActivityResponse.builder()
                .id(a.getId())
                .type(a.getType())
                .subject(a.getSubject())
                .body(a.getBody())
                .clientId(a.getClient() != null ? a.getClient().getId() : null)
                .leadId(a.getLead() != null ? a.getLead().getId() : null)
                .opportunityId(a.getOpportunity() != null ? a.getOpportunity().getId() : null)
                .performedById(a.getPerformedBy() != null ? a.getPerformedBy().getId() : null)
                .performedByName(a.getPerformedBy() != null ? a.getPerformedBy().getFullName() : null)
                .activityDate(a.getActivityDate())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
