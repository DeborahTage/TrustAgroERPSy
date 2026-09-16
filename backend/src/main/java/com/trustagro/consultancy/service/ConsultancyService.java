package com.trustagro.consultancy.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.BusinessException;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.consultancy.dto.*;
import com.trustagro.consultancy.entity.*;
import com.trustagro.consultancy.repository.*;
import com.trustagro.crm.entity.CrmClient;
import com.trustagro.crm.entity.CrmQuotation;
import com.trustagro.crm.repository.CrmClientRepository;
import com.trustagro.crm.repository.CrmQuotationRepository;
import com.trustagro.finance.entity.*;
import com.trustagro.finance.repository.InvoiceRepository;
import com.trustagro.finance.service.FinanceService;
import com.trustagro.notification.service.NotificationService;
import com.trustagro.security.Permissions;
import com.trustagro.user.entity.Role;
import com.trustagro.user.entity.User;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
public class ConsultancyService {

    public static final String MODULE = "CONSULTANCY";
    public static final String SOURCE_MODULE = FinanceSourceTypes.MODULE_CONSULTANCY;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String PROFIT_NOTE = "Profit unavailable";

    private final ConsultancyServiceCatalogRepository serviceCatalogRepository;
    private final ConsultancyRequestRepository requestRepository;
    private final ConsultancyProposalRepository proposalRepository;
    private final ConsultancyEngagementRepository engagementRepository;
    private final ConsultancyAssignmentRepository assignmentRepository;
    private final ConsultancyActivityRepository activityRepository;
    private final ConsultancyDeliverableRepository deliverableRepository;
    private final ConsultancyContractRepository contractRepository;
    private final ConsultancyPaymentRepository paymentRepository;
    private final CrmClientRepository clientRepository;
    private final CrmQuotationRepository crmQuotationRepository;
    private final InvoiceRepository invoiceRepository;
    private final FinanceService financeService;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ─── Access control ──────────────────────────────────────────

    public boolean hasBroadConsultancyAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        Set<String> authorities = new HashSet<>();
        for (GrantedAuthority ga : auth.getAuthorities()) {
            authorities.add(ga.getAuthority());
        }
        if (authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_GENERAL_MANAGER")
                || authorities.contains("ADMIN") || authorities.contains("GENERAL_MANAGER")) {
            return true;
        }
        return authorities.contains(Permissions.CONSULTANCY_ASSIGN)
                || authorities.contains(Permissions.CONSULTANCY_APPROVE)
                || authorities.contains(Permissions.CONSULTANCY_INVOICE)
                || authorities.contains(Permissions.CONSULTANCY_PAYMENT)
                || authorities.contains(Permissions.FINANCE_CREATE)
                || (authorities.contains(Permissions.REPORTS_READ)
                    && authorities.contains(Permissions.CONSULTANCY_REPORT));
    }

    public boolean isAssigned(ConsultancyEngagement e, User u) {
        if (e == null || u == null || u.getId() == null) {
            return false;
        }
        if (e.getLeadConsultant() != null && u.getId().equals(e.getLeadConsultant().getId())) {
            return true;
        }
        return assignmentRepository.existsByEngagementIdAndConsultantIdAndStatusIn(
                e.getId(), u.getId(),
                List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.ACTIVE));
    }

    public void assertCanAccessEngagement(ConsultancyEngagement e) {
        if (hasBroadConsultancyAccess()) {
            return;
        }
        User u = currentUser();
        if (isAssigned(e, u)) {
            return;
        }
        throw new BusinessException("Not authorized to access this engagement");
    }

    private void assertHasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            throw new BusinessException("Not authorized");
        }
        boolean ok = auth.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
        if (!ok) {
            throw new BusinessException("Not authorized: requires " + authority);
        }
    }

    // ─── Dashboard / reports ─────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        Map<String, Object> dash = new LinkedHashMap<>();
        dash.put("activeEngagements", engagementRepository.countByStatus(EngagementStatus.ACTIVE));
        dash.put("onHoldEngagements", engagementRepository.countByStatus(EngagementStatus.ON_HOLD));
        dash.put("completedEngagements", engagementRepository.countByStatus(EngagementStatus.COMPLETED));
        dash.put("openRequests", requestRepository.countByStatus(RequestStatus.NEW)
                + requestRepository.countByStatus(RequestStatus.QUALIFYING)
                + requestRepository.countByStatus(RequestStatus.ASSIGNED)
                + requestRepository.countByStatus(RequestStatus.PROPOSAL_REQUIRED));
        dash.put("totalEngagements", engagementRepository.count());
        BigDecimal outstanding = engagementRepository.findAll().stream()
                .filter(e -> e.getStatus() != EngagementStatus.CANCELLED)
                .map(e -> nvl(e.getOutstandingAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dash.put("outstandingAmount", outstanding);
        dash.put("revenue", consultancyRevenueFromFinance());
        return dash;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reports() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("consultancyRevenueFromFinance", consultancyRevenueFromFinance());
        report.put("engagementsByStatus", Map.of(
                "ACTIVE", engagementRepository.countByStatus(EngagementStatus.ACTIVE),
                "COMPLETED", engagementRepository.countByStatus(EngagementStatus.COMPLETED),
                "ON_HOLD", engagementRepository.countByStatus(EngagementStatus.ON_HOLD),
                "CANCELLED", engagementRepository.countByStatus(EngagementStatus.CANCELLED)
        ));
        report.put("requestsOpen", requestRepository.countByStatus(RequestStatus.NEW)
                + requestRepository.countByStatus(RequestStatus.QUALIFYING)
                + requestRepository.countByStatus(RequestStatus.ASSIGNED)
                + requestRepository.countByStatus(RequestStatus.PROPOSAL_REQUIRED));
        report.put("deliverablesPendingApproval", deliverableRepository.findAll().stream()
                .filter(d -> d.getStatus() == DeliverableStatus.SUBMITTED).count());
        report.put("note", "Revenue is taken from Finance INCOME postings with sourceModule=CONSULTANCY. Profit unavailable (no cost data).");
        return report;
    }

    private BigDecimal consultancyRevenueFromFinance() {
        return financeService.getAllTransactions().stream()
                .filter(t -> SOURCE_MODULE.equals(t.getSourceModule()))
                .filter(t -> t.getTransactionType() == TransactionType.INCOME)
                .map(FinanceTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ─── Service catalog ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ServiceCatalogResponse> listServices(boolean activeOnly) {
        List<ConsultancyServiceCatalog> list = activeOnly
                ? serviceCatalogRepository.findByActiveTrueOrderByNameAsc()
                : serviceCatalogRepository.findAll();
        return list.stream().map(this::toServiceResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServiceCatalogResponse getService(Long id) {
        return toServiceResponse(requireService(id));
    }

    @Transactional
    public ServiceCatalogResponse createService(ServiceCatalogRequest req) {
        String code = req.getServiceCode() != null && !req.getServiceCode().isBlank()
                ? req.getServiceCode().trim().toUpperCase()
                : nextServiceCode();
        if (serviceCatalogRepository.existsByServiceCode(code)) {
            throw new BusinessException("Service code already exists: " + code);
        }
        if (req.getDefaultPrice() == null || req.getDefaultPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Default price must be zero or greater");
        }
        ConsultancyServiceCatalog s = new ConsultancyServiceCatalog();
        applyService(s, req, code);
        s.setCreatedBy(currentUser());
        ConsultancyServiceCatalog saved = serviceCatalogRepository.save(s);
        auditService.log("CONSULTANCY_SERVICE_CREATED", MODULE, "ConsultancyServiceCatalog",
                saved.getId(), null, saved.getServiceCode());
        return toServiceResponse(saved);
    }

    @Transactional
    public ServiceCatalogResponse updateService(Long id, ServiceCatalogRequest req) {
        ConsultancyServiceCatalog s = requireService(id);
        String code = req.getServiceCode() != null && !req.getServiceCode().isBlank()
                ? req.getServiceCode().trim().toUpperCase()
                : s.getServiceCode();
        if (!code.equals(s.getServiceCode()) && serviceCatalogRepository.existsByServiceCode(code)) {
            throw new BusinessException("Service code already exists: " + code);
        }
        applyService(s, req, code);
        ConsultancyServiceCatalog saved = serviceCatalogRepository.save(s);
        auditService.log("CONSULTANCY_SERVICE_UPDATED", MODULE, "ConsultancyServiceCatalog",
                saved.getId(), null, saved.getServiceCode());
        return toServiceResponse(saved);
    }

    // ─── Requests ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RequestResponse> listRequests(Long customerId, RequestStatus status) {
        List<ConsultancyRequest> list;
        if (customerId != null) {
            list = requestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        } else if (status != null) {
            list = requestRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = requestRepository.findAll();
        }
        return list.stream()
                .sorted(Comparator.comparing(ConsultancyRequest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toRequestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RequestResponse getRequest(Long id) {
        return toRequestResponse(requireRequest(id));
    }

    @Transactional
    public RequestResponse createRequest(RequestCreateRequest req) {
        CrmClient customer = requireClient(req.getCustomerId());
        ConsultancyRequest r = new ConsultancyRequest();
        r.setRequestNumber(nextRequestNumber());
        r.setCustomer(customer);
        r.setSubject(req.getSubject().trim());
        r.setDescription(req.getDescription());
        if (req.getServiceId() != null) {
            r.setRequestedService(requireService(req.getServiceId()));
        }
        r.setRequestedDate(req.getRequestedDate() != null ? req.getRequestedDate() : LocalDate.now());
        r.setPreferredLocation(req.getPreferredLocation());
        r.setPriority(req.getPriority() != null ? req.getPriority() : RequestPriority.MEDIUM);
        r.setStatus(RequestStatus.NEW);
        r.setNotes(req.getNotes());
        r.setCreatedBy(currentUser());
        ConsultancyRequest saved = requestRepository.save(r);
        auditService.log("CONSULTANCY_REQUEST_CREATED", MODULE, "ConsultancyRequest",
                saved.getId(), null, saved.getRequestNumber());
        notificationService.createRoleNotification(
                "Consultancy request created",
                saved.getRequestNumber() + " — " + saved.getSubject(),
                Role.CONSULTANT);
        notificationService.createRoleNotification(
                "Consultancy request created",
                saved.getRequestNumber() + " — " + saved.getSubject(),
                Role.CRM_MANAGER);
        return toRequestResponse(saved);
    }

    @Transactional
    public RequestResponse updateRequest(Long id, RequestUpdateRequest req) {
        ConsultancyRequest r = requireRequest(id);
        if (r.getStatus() == RequestStatus.CONVERTED || r.getStatus() == RequestStatus.REJECTED
                || r.getStatus() == RequestStatus.CANCELLED) {
            throw new BusinessException("Cannot update request in status " + r.getStatus());
        }
        if (req.getSubject() != null && !req.getSubject().isBlank()) {
            r.setSubject(req.getSubject().trim());
        }
        if (req.getDescription() != null) r.setDescription(req.getDescription());
        if (req.getServiceId() != null) r.setRequestedService(requireService(req.getServiceId()));
        if (req.getRequestedDate() != null) r.setRequestedDate(req.getRequestedDate());
        if (req.getPreferredLocation() != null) r.setPreferredLocation(req.getPreferredLocation());
        if (req.getPriority() != null) r.setPriority(req.getPriority());
        if (req.getNotes() != null) r.setNotes(req.getNotes());
        ConsultancyRequest saved = requestRepository.save(r);
        auditService.log("CONSULTANCY_REQUEST_UPDATED", MODULE, "ConsultancyRequest",
                saved.getId(), null, saved.getRequestNumber());
        return toRequestResponse(saved);
    }

    @Transactional
    public RequestResponse assignRequest(Long id, AssignmentRequest req) {
        ConsultancyRequest r = requireRequest(id);
        RequestLifecycle.assertTransition(r.getStatus(), RequestStatus.ASSIGNED);
        User consultant = requireUser(req.getConsultantId());
        r.setAssignedConsultant(consultant);
        r.setStatus(RequestStatus.ASSIGNED);
        ConsultancyRequest saved = requestRepository.save(r);
        auditService.log("CONSULTANCY_REQUEST_ASSIGNED", MODULE, "ConsultancyRequest",
                saved.getId(), null, consultant.getEmail());
        notificationService.createRoleNotification(
                "Consultancy request assigned",
                saved.getRequestNumber() + " assigned to " + consultant.getFullName(),
                Role.CONSULTANT);
        return toRequestResponse(saved);
    }

    @Transactional
    public RequestResponse rejectRequest(Long id, CompleteRequest body) {
        ConsultancyRequest r = requireRequest(id);
        RequestLifecycle.assertTransition(r.getStatus(), RequestStatus.REJECTED);
        RequestStatus from = r.getStatus();
        r.setStatus(RequestStatus.REJECTED);
        if (body != null && body.getCompletionNotes() != null) {
            r.setNotes(body.getCompletionNotes());
        }
        ConsultancyRequest saved = requestRepository.save(r);
        auditService.log("CONSULTANCY_REQUEST_REJECTED", MODULE, "ConsultancyRequest",
                saved.getId(), from.name(), RequestStatus.REJECTED.name());
        return toRequestResponse(saved);
    }

    @Transactional
    public RequestResponse cancelRequest(Long id) {
        ConsultancyRequest r = requireRequest(id);
        if (r.getStatus() == RequestStatus.CANCELLED) {
            return toRequestResponse(r);
        }
        RequestLifecycle.assertTransition(r.getStatus(), RequestStatus.CANCELLED);
        RequestStatus from = r.getStatus();
        r.setStatus(RequestStatus.CANCELLED);
        ConsultancyRequest saved = requestRepository.save(r);
        auditService.log("CONSULTANCY_REQUEST_CANCELLED", MODULE, "ConsultancyRequest",
                saved.getId(), from.name(), RequestStatus.CANCELLED.name());
        return toRequestResponse(saved);
    }

    @Transactional
    public ProposalResponse convertRequestToProposal(Long id, ProposalRequest req) {
        ConsultancyRequest r = requireRequest(id);
        if (r.getStatus() == RequestStatus.CONVERTED) {
            throw new BusinessException("Request already converted");
        }
        if (r.getStatus() != RequestStatus.PROPOSAL_REQUIRED) {
            RequestLifecycle.assertTransition(r.getStatus(), RequestStatus.PROPOSAL_REQUIRED);
            r.setStatus(RequestStatus.PROPOSAL_REQUIRED);
            requestRepository.save(r);
        }
        ProposalRequest pr = req != null ? req : new ProposalRequest();
        if (pr.getCustomerId() == null) pr.setCustomerId(r.getCustomer().getId());
        if (pr.getRequestId() == null) pr.setRequestId(r.getId());
        if (pr.getServiceId() == null && r.getRequestedService() != null) {
            pr.setServiceId(r.getRequestedService().getId());
        }
        if (pr.getTitle() == null || pr.getTitle().isBlank()) {
            pr.setTitle(r.getSubject());
        }
        if (pr.getProposedAmount() == null) {
            pr.setProposedAmount(r.getRequestedService() != null
                    ? nvl(r.getRequestedService().getDefaultPrice()) : ZERO);
        }
        return createProposal(pr);
    }

    @Transactional
    public EngagementResponse convertRequestToEngagement(Long id, EngagementRequest req) {
        ConsultancyRequest r = requireRequest(id);
        if (r.getStatus() == RequestStatus.CONVERTED) {
            throw new BusinessException("Request already converted");
        }
        EngagementRequest er = req != null ? req : new EngagementRequest();
        if (er.getCustomerId() == null) er.setCustomerId(r.getCustomer().getId());
        er.setRequestId(r.getId());
        if (er.getServiceId() == null && r.getRequestedService() != null) {
            er.setServiceId(r.getRequestedService().getId());
        }
        if (er.getTitle() == null || er.getTitle().isBlank()) {
            er.setTitle(r.getSubject());
        }
        if (er.getAgreedAmount() == null) {
            er.setAgreedAmount(r.getRequestedService() != null
                    ? nvl(r.getRequestedService().getDefaultPrice()) : ZERO);
        }
        EngagementResponse created = createEngagement(er);
        RequestLifecycle.assertTransition(r.getStatus(), RequestStatus.CONVERTED);
        r.setStatus(RequestStatus.CONVERTED);
        requestRepository.save(r);
        return created;
    }

    /**
     * Idempotent CRM quotation → request → engagement → contract chain.
     * Resumes incomplete conversions without duplicating rows keyed by crmQuotationId.
     */
    @Transactional
    public CrmQuotationConversionResult convertFromCrmQuotation(Long crmQuotationId) {
        CrmQuotation quotation = crmQuotationRepository.findById(crmQuotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found: " + crmQuotationId));

        ConsultancyRequest request = requestRepository.findByCrmQuotationId(crmQuotationId)
                .orElseGet(() -> createRequestFromQuotation(quotation));

        ConsultancyEngagement engagement;
        if (request.getStatus() == RequestStatus.CONVERTED) {
            engagement = engagementRepository.findByRequestId(request.getId())
                    .orElseThrow(() -> new BusinessException(
                            "Request " + request.getRequestNumber() + " is CONVERTED but has no engagement"));
        } else {
            EngagementRequest er = new EngagementRequest();
            er.setCustomerId(quotation.getClient().getId());
            er.setRequestId(request.getId());
            er.setTitle("From quotation " + quotation.getQuotationNumber());
            er.setDescription(quotation.getNotes());
            er.setAgreedAmount(nvl(quotation.getTotalAmount()));
            er.setCurrency(quotation.getCurrency() != null ? quotation.getCurrency() : "ETB");
            er.setNotes("Auto-created from CRM quotation " + quotation.getQuotationNumber());
            EngagementResponse engResp = convertRequestToEngagement(request.getId(), er);
            engagement = requireEngagement(engResp.getId());
        }

        ConsultancyContract contract = contractRepository.findByEngagementId(engagement.getId())
                .orElseGet(() -> createContractForCrmQuotation(engagement, quotation));

        return new CrmQuotationConversionResult(request.getId(), engagement.getId(), contract.getId());
    }

    private ConsultancyRequest createRequestFromQuotation(CrmQuotation quotation) {
        ConsultancyRequest r = new ConsultancyRequest();
        r.setRequestNumber(nextRequestNumber());
        r.setCustomer(quotation.getClient());
        r.setSubject("From quotation " + quotation.getQuotationNumber());
        r.setDescription(quotation.getNotes());
        r.setRequestedDate(LocalDate.now());
        r.setPriority(RequestPriority.MEDIUM);
        r.setStatus(RequestStatus.NEW);
        r.setNotes("Auto-created from CRM quotation " + quotation.getQuotationNumber());
        r.setCrmQuotationId(quotation.getId());
        r.setCreatedBy(currentUser());
        try {
            ConsultancyRequest saved = requestRepository.save(r);
            auditService.log("CONSULTANCY_REQUEST_CREATED", MODULE, "ConsultancyRequest",
                    saved.getId(), null, saved.getRequestNumber());
            return saved;
        } catch (DataIntegrityViolationException | ConstraintViolationException ex) {
            return requestRepository.findByCrmQuotationId(quotation.getId())
                    .orElseThrow(() -> ex);
        }
    }

    /** Creates a draft contract for CRM conversion without consultancy RBAC gate (caller is CRM accept). */
    private ConsultancyContract createContractForCrmQuotation(ConsultancyEngagement e, CrmQuotation quotation) {
        if (contractRepository.existsByEngagementId(e.getId())) {
            return contractRepository.findByEngagementId(e.getId()).orElseThrow();
        }
        ConsultancyContract c = new ConsultancyContract();
        c.setContractNumber(nextContractNumber());
        c.setEngagement(e);
        c.setCustomer(e.getCustomer());
        c.setTitle("From quotation " + quotation.getQuotationNumber());
        c.setScope(quotation.getNotes());
        c.setStartDate(LocalDate.now());
        c.setContractValue(nvl(quotation.getTotalAmount()).setScale(2, RoundingMode.HALF_UP));
        c.setCurrency(quotation.getCurrency() != null ? quotation.getCurrency() : "ETB");
        c.setStatus(ContractStatus.DRAFT);
        c.setNotes("Auto-created from CRM quotation " + quotation.getQuotationNumber());
        c.setCreatedBy(currentUser());
        try {
            ConsultancyContract saved = contractRepository.save(c);
            auditService.log("CONSULTANCY_CONTRACT_CREATED", MODULE, "ConsultancyContract",
                    saved.getId(), null, saved.getContractNumber());
            return saved;
        } catch (DataIntegrityViolationException | ConstraintViolationException ex) {
            return contractRepository.findByEngagementId(e.getId())
                    .orElseThrow(() -> ex);
        }
    }

    // ─── Proposals ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProposalResponse> listProposals(Long customerId, Long requestId) {
        List<ConsultancyProposal> list;
        if (requestId != null) {
            list = proposalRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
        } else if (customerId != null) {
            list = proposalRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        } else {
            list = proposalRepository.findAll();
        }
        return list.stream().map(this::toProposalResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProposalResponse getProposal(Long id) {
        return toProposalResponse(requireProposal(id));
    }

    @Transactional
    public ProposalResponse createProposal(ProposalRequest req) {
        CrmClient customer = requireClient(req.getCustomerId());
        ConsultancyProposal p = new ConsultancyProposal();
        p.setProposalNumber(nextProposalNumber());
        p.setCustomer(customer);
        if (req.getRequestId() != null) {
            p.setRequest(requireRequest(req.getRequestId()));
        }
        if (req.getServiceId() != null) {
            p.setService(requireService(req.getServiceId()));
        }
        p.setTitle(req.getTitle().trim());
        p.setScope(req.getScope());
        p.setProposedAmount(nvl(req.getProposedAmount()).setScale(2, RoundingMode.HALF_UP));
        p.setCurrency(req.getCurrency() != null && !req.getCurrency().isBlank() ? req.getCurrency() : "ETB");
        p.setValidUntil(req.getValidUntil());
        p.setStatus(ProposalStatus.DRAFT);
        p.setNotes(req.getNotes());
        p.setCreatedBy(currentUser());
        ConsultancyProposal saved = proposalRepository.save(p);
        auditService.log("CONSULTANCY_PROPOSAL_CREATED", MODULE, "ConsultancyProposal",
                saved.getId(), null, saved.getProposalNumber());
        return toProposalResponse(saved);
    }

    @Transactional
    public ProposalResponse updateProposal(Long id, ProposalRequest req) {
        ConsultancyProposal p = requireProposal(id);
        if (!ProposalLifecycle.isEditable(p.getStatus())) {
            throw new BusinessException("Proposal cannot be edited in status " + p.getStatus());
        }
        if (req.getTitle() != null && !req.getTitle().isBlank()) p.setTitle(req.getTitle().trim());
        if (req.getScope() != null) p.setScope(req.getScope());
        if (req.getProposedAmount() != null) {
            p.setProposedAmount(req.getProposedAmount().setScale(2, RoundingMode.HALF_UP));
        }
        if (req.getCurrency() != null) p.setCurrency(req.getCurrency());
        if (req.getValidUntil() != null) p.setValidUntil(req.getValidUntil());
        if (req.getNotes() != null) p.setNotes(req.getNotes());
        if (req.getServiceId() != null) p.setService(requireService(req.getServiceId()));
        ConsultancyProposal saved = proposalRepository.save(p);
        auditService.log("CONSULTANCY_PROPOSAL_UPDATED", MODULE, "ConsultancyProposal",
                saved.getId(), null, saved.getProposalNumber());
        return toProposalResponse(saved);
    }

    @Transactional
    public ProposalResponse sendProposal(Long id) {
        ConsultancyProposal p = requireProposal(id);
        ProposalLifecycle.assertTransition(p.getStatus(), ProposalStatus.SENT);
        p.setStatus(ProposalStatus.SENT);
        ConsultancyProposal saved = proposalRepository.save(p);
        auditService.log("CONSULTANCY_PROPOSAL_SENT", MODULE, "ConsultancyProposal",
                saved.getId(), ProposalStatus.DRAFT.name(), ProposalStatus.SENT.name());
        notificationService.createRoleNotification(
                "Consultancy proposal sent",
                saved.getProposalNumber() + " — " + saved.getTitle(),
                Role.CRM_MANAGER);
        return toProposalResponse(saved);
    }

    @Transactional
    public ProposalResponse acceptProposal(Long id) {
        ConsultancyProposal p = requireProposal(id);
        // Idempotent: already accepted with engagement
        if (p.getStatus() == ProposalStatus.ACCEPTED && p.getConvertedEngagementId() != null) {
            return toProposalResponse(p);
        }
        ProposalLifecycle.assertTransition(p.getStatus(), ProposalStatus.ACCEPTED);

        EngagementRequest er = new EngagementRequest();
        er.setCustomerId(p.getCustomer().getId());
        er.setProposalId(p.getId());
        if (p.getRequest() != null) er.setRequestId(p.getRequest().getId());
        if (p.getService() != null) er.setServiceId(p.getService().getId());
        er.setTitle(p.getTitle());
        er.setScope(p.getScope());
        er.setAgreedAmount(nvl(p.getProposedAmount()));
        er.setCurrency(p.getCurrency());
        er.setBillingType(BillingType.FIXED_PRICE);
        er.setNotes(p.getNotes());

        ConsultancyEngagement eng = buildEngagementEntity(er);
        eng.setStatus(EngagementStatus.APPROVED);
        eng = engagementRepository.save(eng);

        p.setStatus(ProposalStatus.ACCEPTED);
        p.setConvertedEngagementId(eng.getId());
        proposalRepository.save(p);

        if (p.getRequest() != null) {
            ConsultancyRequest req = p.getRequest();
            if (req.getStatus() != RequestStatus.CONVERTED
                    && req.getStatus() != RequestStatus.REJECTED
                    && req.getStatus() != RequestStatus.CANCELLED) {
                RequestLifecycle.assertTransition(req.getStatus(), RequestStatus.CONVERTED);
                req.setStatus(RequestStatus.CONVERTED);
                requestRepository.save(req);
            }
        }

        auditService.log("CONSULTANCY_PROPOSAL_ACCEPTED", MODULE, "ConsultancyProposal",
                p.getId(), ProposalStatus.SENT.name(), eng.getEngagementNumber());
        notificationService.createRoleNotification(
                "Consultancy proposal accepted",
                p.getProposalNumber() + " → " + eng.getEngagementNumber(),
                Role.GENERAL_MANAGER);
        notificationService.createRoleNotification(
                "Consultancy proposal accepted",
                p.getProposalNumber() + " → " + eng.getEngagementNumber(),
                Role.CONSULTANT);
        return toProposalResponse(p);
    }

    @Transactional
    public ProposalResponse rejectProposal(Long id, CompleteRequest body) {
        ConsultancyProposal p = requireProposal(id);
        ProposalLifecycle.assertTransition(p.getStatus(), ProposalStatus.REJECTED);
        p.setStatus(ProposalStatus.REJECTED);
        if (body != null && body.getCompletionNotes() != null) {
            p.setNotes(body.getCompletionNotes());
        }
        ConsultancyProposal saved = proposalRepository.save(p);
        auditService.log("CONSULTANCY_PROPOSAL_REJECTED", MODULE, "ConsultancyProposal",
                saved.getId(), ProposalStatus.SENT.name(), ProposalStatus.REJECTED.name());
        return toProposalResponse(saved);
    }

    // ─── Engagements ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EngagementResponse> listEngagements(Long customerId, EngagementStatus status) {
        List<ConsultancyEngagement> list;
        if (customerId != null) {
            list = engagementRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        } else if (status != null) {
            list = engagementRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = engagementRepository.findAll();
        }
        User me = currentUser();
        boolean broad = hasBroadConsultancyAccess();
        return list.stream()
                .filter(e -> broad || isAssigned(e, me))
                .map(e -> toEngagementResponse(e, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public EngagementResponse getEngagement(Long id) {
        ConsultancyEngagement e = requireEngagement(id);
        assertCanAccessEngagement(e);
        return toEngagementResponse(e, true);
    }

    @Transactional
    public EngagementResponse createEngagement(EngagementRequest req) {
        ConsultancyEngagement e = buildEngagementEntity(req);
        e.setStatus(EngagementStatus.DRAFT);
        ConsultancyEngagement saved = engagementRepository.save(e);
        auditService.log("CONSULTANCY_ENGAGEMENT_CREATED", MODULE, "ConsultancyEngagement",
                saved.getId(), null, saved.getEngagementNumber());
        notificationService.createRoleNotification(
                "Consultancy engagement created",
                saved.getEngagementNumber() + " — " + saved.getTitle(),
                Role.CONSULTANT);
        return toEngagementResponse(saved, false);
    }

    @Transactional
    public EngagementResponse updateEngagement(Long id, EngagementRequest req) {
        ConsultancyEngagement e = requireEngagement(id);
        assertCanAccessEngagement(e);
        if (!EngagementLifecycle.isEditable(e.getStatus())) {
            throw new BusinessException("Engagement cannot be edited in status " + e.getStatus());
        }
        applyEngagementFields(e, req);
        ConsultancyEngagement saved = engagementRepository.save(e);
        auditService.log("CONSULTANCY_ENGAGEMENT_UPDATED", MODULE, "ConsultancyEngagement",
                saved.getId(), null, saved.getEngagementNumber());
        return toEngagementResponse(saved, false);
    }

    @Transactional
    public EngagementResponse proposeEngagement(Long id) {
        return transitionEngagement(id, EngagementStatus.PROPOSED, "CONSULTANCY_ENGAGEMENT_PROPOSED");
    }

    @Transactional
    public EngagementResponse approveEngagement(Long id) {
        return transitionEngagement(id, EngagementStatus.APPROVED, "CONSULTANCY_ENGAGEMENT_APPROVED");
    }

    @Transactional
    public EngagementResponse activateEngagement(Long id) {
        return transitionEngagement(id, EngagementStatus.ACTIVE, "CONSULTANCY_ENGAGEMENT_ACTIVATED");
    }

    @Transactional
    public EngagementResponse holdEngagement(Long id) {
        return transitionEngagement(id, EngagementStatus.ON_HOLD, "CONSULTANCY_ENGAGEMENT_ON_HOLD");
    }

    @Transactional
    public EngagementResponse resumeEngagement(Long id) {
        return transitionEngagement(id, EngagementStatus.ACTIVE, "CONSULTANCY_ENGAGEMENT_RESUMED");
    }

    @Transactional
    public EngagementResponse completeEngagement(Long id, CompleteRequest body) {
        ConsultancyEngagement e = requireEngagement(id);
        assertCanAccessEngagement(e);
        EngagementLifecycle.assertTransition(e.getStatus(), EngagementStatus.COMPLETED);
        EngagementStatus from = e.getStatus();
        e.setStatus(EngagementStatus.COMPLETED);
        e.setCompletedAt(LocalDateTime.now());
        e.setCompletedBy(currentUser());
        e.setActualEndDate(LocalDate.now());
        if (body != null) {
            e.setCompletionNotes(body.getCompletionNotes());
        }
        ConsultancyEngagement saved = engagementRepository.save(e);
        auditService.log("CONSULTANCY_ENGAGEMENT_COMPLETED", MODULE, "ConsultancyEngagement",
                saved.getId(), from.name(), EngagementStatus.COMPLETED.name());
        notificationService.createRoleNotification(
                "Consultancy engagement completed",
                saved.getEngagementNumber() + " — " + saved.getTitle(),
                Role.GENERAL_MANAGER);
        notificationService.createRoleNotification(
                "Consultancy engagement completed",
                saved.getEngagementNumber() + " — " + saved.getTitle(),
                Role.FINANCE);
        return toEngagementResponse(saved, true);
    }

    @Transactional
    public EngagementResponse cancelEngagement(Long id) {
        ConsultancyEngagement e = requireEngagement(id);
        assertCanAccessEngagement(e);
        if (e.getStatus() == EngagementStatus.CANCELLED) {
            return toEngagementResponse(e, true);
        }
        if (e.getPaidAmount() != null && e.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Cannot cancel engagement with payments; reverse payments first");
        }
        EngagementLifecycle.assertTransition(e.getStatus(), EngagementStatus.CANCELLED);
        EngagementStatus from = e.getStatus();
        e.setStatus(EngagementStatus.CANCELLED);
        if (e.getInvoice() != null && e.getInvoice().getStatus() != InvoiceStatus.CANCELLED
                && (e.getInvoice().getPaidAmount() == null
                || e.getInvoice().getPaidAmount().compareTo(BigDecimal.ZERO) == 0)) {
            Invoice inv = e.getInvoice();
            inv.setStatus(InvoiceStatus.CANCELLED);
            invoiceRepository.save(inv);
        }
        ConsultancyEngagement saved = engagementRepository.save(e);
        auditService.log("CONSULTANCY_ENGAGEMENT_CANCELLED", MODULE, "ConsultancyEngagement",
                saved.getId(), from.name(), EngagementStatus.CANCELLED.name());
        return toEngagementResponse(saved, true);
    }

    private EngagementResponse transitionEngagement(Long id, EngagementStatus target, String auditAction) {
        ConsultancyEngagement e = requireEngagement(id);
        assertCanAccessEngagement(e);
        EngagementLifecycle.assertTransition(e.getStatus(), target);
        EngagementStatus from = e.getStatus();
        e.setStatus(target);
        ConsultancyEngagement saved = engagementRepository.save(e);
        auditService.log(auditAction, MODULE, "ConsultancyEngagement",
                saved.getId(), from.name(), target.name());
        if (target == EngagementStatus.APPROVED || target == EngagementStatus.ACTIVE) {
            notificationService.createRoleNotification(
                    "Consultancy engagement " + target.name().toLowerCase().replace('_', ' '),
                    saved.getEngagementNumber() + " — " + saved.getTitle(),
                    Role.CONSULTANT);
            notificationService.createRoleNotification(
                    "Consultancy engagement update",
                    saved.getEngagementNumber() + " is now " + target,
                    Role.GENERAL_MANAGER);
        }
        return toEngagementResponse(saved, false);
    }

    @Transactional
    public EngagementResponse assignConsultant(Long engagementId, AssignmentRequest req) {
        ConsultancyEngagement e = requireEngagement(engagementId);
        assertCanAccessEngagement(e);
        if (e.getStatus() == EngagementStatus.CANCELLED || e.getStatus() == EngagementStatus.COMPLETED) {
            throw new BusinessException("Cannot assign consultant to engagement in status " + e.getStatus());
        }
        User consultant = requireUser(req.getConsultantId());
        boolean exists = assignmentRepository.existsByEngagementIdAndConsultantIdAndStatusIn(
                e.getId(), consultant.getId(),
                List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.ACTIVE));
        if (!exists) {
            ConsultancyAssignment a = new ConsultancyAssignment();
            a.setEngagement(e);
            a.setConsultant(consultant);
            a.setRole(req.getRole() != null ? req.getRole() : AssignmentRole.LEAD_CONSULTANT);
            a.setAssignedDate(LocalDate.now());
            a.setStartDate(req.getStartDate());
            a.setEndDate(req.getEndDate());
            a.setStatus(AssignmentStatus.ASSIGNED);
            a.setNotes(req.getNotes());
            a.setAssignedBy(currentUser());
            assignmentRepository.save(a);
        }
        if (req.isAsLead() || e.getLeadConsultant() == null) {
            e.setLeadConsultant(consultant);
            engagementRepository.save(e);
        }
        auditService.log("CONSULTANCY_CONSULTANT_ASSIGNED", MODULE, "ConsultancyEngagement",
                e.getId(), null, consultant.getEmail());
        notificationService.createRoleNotification(
                "Consultant assigned to engagement",
                e.getEngagementNumber() + " → " + consultant.getFullName(),
                Role.CONSULTANT);
        return toEngagementResponse(e, true);
    }

    // ─── Activities ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ActivityResponse> listActivities(Long engagementId) {
        ConsultancyEngagement e = requireEngagement(engagementId);
        assertCanAccessEngagement(e);
        return activityRepository.findByEngagementIdOrderByActivityDateDesc(engagementId).stream()
                .map(this::toActivityResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ActivityResponse getActivity(Long id) {
        ConsultancyActivity a = requireActivity(id);
        assertCanAccessEngagement(a.getEngagement());
        return toActivityResponse(a);
    }

    @Transactional
    public ActivityResponse createActivity(ActivityRequest req) {
        ConsultancyEngagement e = requireEngagement(req.getEngagementId());
        assertCanAccessEngagement(e);
        if (e.getStatus() != EngagementStatus.ACTIVE && e.getStatus() != EngagementStatus.APPROVED
                && e.getStatus() != EngagementStatus.ON_HOLD) {
            throw new BusinessException("Activities require an active or approved engagement");
        }
        ConsultancyActivity a = new ConsultancyActivity();
        a.setActivityNumber(nextActivityNumber());
        a.setEngagement(e);
        a.setCustomer(e.getCustomer());
        if (req.getConsultantId() != null) {
            a.setConsultant(requireUser(req.getConsultantId()));
        } else {
            a.setConsultant(currentUser());
        }
        a.setActivityType(req.getActivityType() != null ? req.getActivityType() : ActivityType.OTHER);
        a.setActivityDate(req.getActivityDate());
        a.setStartTime(req.getStartTime());
        a.setEndTime(req.getEndTime());
        a.setDurationMinutes(req.getDurationMinutes());
        a.setLocation(req.getLocation());
        a.setDescription(req.getDescription().trim());
        a.setFindings(req.getFindings());
        a.setRecommendations(req.getRecommendations());
        a.setCustomerNotes(req.getCustomerNotes());
        a.setStatus(ActivityStatus.PLANNED);
        a.setCreatedBy(currentUser());
        ConsultancyActivity saved = activityRepository.save(a);
        auditService.log("CONSULTANCY_ACTIVITY_CREATED", MODULE, "ConsultancyActivity",
                saved.getId(), null, saved.getActivityNumber());
        return toActivityResponse(saved);
    }

    @Transactional
    public ActivityResponse updateActivity(Long id, ActivityRequest req) {
        ConsultancyActivity a = requireActivity(id);
        assertCanAccessEngagement(a.getEngagement());
        if (a.getStatus() == ActivityStatus.COMPLETED || a.getStatus() == ActivityStatus.CANCELLED) {
            throw new BusinessException("Cannot update activity in status " + a.getStatus());
        }
        if (req.getActivityType() != null) a.setActivityType(req.getActivityType());
        if (req.getActivityDate() != null) a.setActivityDate(req.getActivityDate());
        if (req.getStartTime() != null) a.setStartTime(req.getStartTime());
        if (req.getEndTime() != null) a.setEndTime(req.getEndTime());
        if (req.getDurationMinutes() != null) a.setDurationMinutes(req.getDurationMinutes());
        if (req.getLocation() != null) a.setLocation(req.getLocation());
        if (req.getDescription() != null && !req.getDescription().isBlank()) {
            a.setDescription(req.getDescription().trim());
        }
        if (req.getFindings() != null) a.setFindings(req.getFindings());
        if (req.getRecommendations() != null) a.setRecommendations(req.getRecommendations());
        if (req.getCustomerNotes() != null) a.setCustomerNotes(req.getCustomerNotes());
        if (req.getConsultantId() != null) a.setConsultant(requireUser(req.getConsultantId()));
        ConsultancyActivity saved = activityRepository.save(a);
        auditService.log("CONSULTANCY_ACTIVITY_UPDATED", MODULE, "ConsultancyActivity",
                saved.getId(), null, saved.getActivityNumber());
        return toActivityResponse(saved);
    }

    @Transactional
    public ActivityResponse startActivity(Long id) {
        ConsultancyActivity a = requireActivity(id);
        assertCanAccessEngagement(a.getEngagement());
        ActivityLifecycle.assertTransition(a.getStatus(), ActivityStatus.IN_PROGRESS);
        a.setStatus(ActivityStatus.IN_PROGRESS);
        return toActivityResponse(activityRepository.save(a));
    }

    @Transactional
    public ActivityResponse completeActivity(Long id, CompleteRequest body) {
        ConsultancyActivity a = requireActivity(id);
        assertCanAccessEngagement(a.getEngagement());
        ActivityLifecycle.assertTransition(a.getStatus(), ActivityStatus.COMPLETED);
        a.setStatus(ActivityStatus.COMPLETED);
        if (body != null && body.getCompletionNotes() != null) {
            a.setFindings(body.getCompletionNotes());
        }
        ConsultancyActivity saved = activityRepository.save(a);
        auditService.log("CONSULTANCY_ACTIVITY_COMPLETED", MODULE, "ConsultancyActivity",
                saved.getId(), null, saved.getActivityNumber());
        return toActivityResponse(saved);
    }

    // ─── Deliverables ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DeliverableResponse> listDeliverables(Long engagementId) {
        ConsultancyEngagement e = requireEngagement(engagementId);
        assertCanAccessEngagement(e);
        return deliverableRepository.findByEngagementIdOrderByDueDateAsc(engagementId).stream()
                .map(this::toDeliverableResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeliverableResponse getDeliverable(Long id) {
        ConsultancyDeliverable d = requireDeliverable(id);
        assertCanAccessEngagement(d.getEngagement());
        return toDeliverableResponse(d);
    }

    @Transactional
    public DeliverableResponse createDeliverable(DeliverableRequest req) {
        ConsultancyEngagement e = requireEngagement(req.getEngagementId());
        assertCanAccessEngagement(e);
        ConsultancyDeliverable d = new ConsultancyDeliverable();
        d.setDeliverableNumber(nextDeliverableNumber());
        d.setEngagement(e);
        d.setTitle(req.getTitle().trim());
        d.setDescription(req.getDescription());
        d.setDueDate(req.getDueDate());
        if (req.getAssignedToId() != null) {
            d.setAssignedTo(requireUser(req.getAssignedToId()));
        }
        d.setBillableAmount(req.getBillableAmount() != null
                ? req.getBillableAmount().setScale(2, RoundingMode.HALF_UP) : ZERO);
        d.setBillable(Boolean.TRUE.equals(req.getBillable()));
        d.setNotes(req.getNotes());
        d.setStatus(DeliverableStatus.PENDING);
        d.setCreatedBy(currentUser());
        ConsultancyDeliverable saved = deliverableRepository.save(d);
        auditService.log("CONSULTANCY_DELIVERABLE_CREATED", MODULE, "ConsultancyDeliverable",
                saved.getId(), null, saved.getDeliverableNumber());
        return toDeliverableResponse(saved);
    }

    @Transactional
    public DeliverableResponse startDeliverable(Long id) {
        ConsultancyDeliverable d = requireDeliverable(id);
        assertCanAccessEngagement(d.getEngagement());
        DeliverableLifecycle.assertTransition(d.getStatus(), DeliverableStatus.IN_PROGRESS);
        d.setStatus(DeliverableStatus.IN_PROGRESS);
        return toDeliverableResponse(deliverableRepository.save(d));
    }

    @Transactional
    public DeliverableResponse submitDeliverable(Long id) {
        ConsultancyDeliverable d = requireDeliverable(id);
        assertCanAccessEngagement(d.getEngagement());
        DeliverableLifecycle.assertTransition(d.getStatus(), DeliverableStatus.SUBMITTED);
        d.setStatus(DeliverableStatus.SUBMITTED);
        d.setSubmittedDate(LocalDate.now());
        ConsultancyDeliverable saved = deliverableRepository.save(d);
        auditService.log("CONSULTANCY_DELIVERABLE_SUBMITTED", MODULE, "ConsultancyDeliverable",
                saved.getId(), null, saved.getDeliverableNumber());
        notificationService.createRoleNotification(
                "Consultancy deliverable submitted",
                saved.getDeliverableNumber() + " — " + saved.getTitle(),
                Role.GENERAL_MANAGER);
        return toDeliverableResponse(saved);
    }

    @Transactional
    public DeliverableResponse approveDeliverable(Long id) {
        assertHasAuthority(Permissions.CONSULTANCY_APPROVE);
        ConsultancyDeliverable d = requireDeliverable(id);
        assertCanAccessEngagement(d.getEngagement());
        DeliverableLifecycle.assertTransition(d.getStatus(), DeliverableStatus.APPROVED);
        d.setStatus(DeliverableStatus.APPROVED);
        d.setApprovedDate(LocalDate.now());
        d.setRejectionReason(null);
        ConsultancyDeliverable saved = deliverableRepository.save(d);
        auditService.log("CONSULTANCY_DELIVERABLE_APPROVED", MODULE, "ConsultancyDeliverable",
                saved.getId(), DeliverableStatus.SUBMITTED.name(), DeliverableStatus.APPROVED.name());

        ConsultancyEngagement eng = saved.getEngagement();
        if (eng.getBillingType() == BillingType.MILESTONE
                && saved.isBillable()
                && nvl(saved.getBillableAmount()).compareTo(BigDecimal.ZERO) > 0
                && saved.getInvoice() == null) {
            billMilestone(saved.getId());
            saved = requireDeliverable(id);
        }
        return toDeliverableResponse(saved);
    }

    @Transactional
    public DeliverableResponse rejectDeliverable(Long id, CompleteRequest body) {
        assertHasAuthority(Permissions.CONSULTANCY_APPROVE);
        ConsultancyDeliverable d = requireDeliverable(id);
        assertCanAccessEngagement(d.getEngagement());
        DeliverableLifecycle.assertTransition(d.getStatus(), DeliverableStatus.REJECTED);
        d.setStatus(DeliverableStatus.REJECTED);
        d.setRejectionReason(body != null ? body.getCompletionNotes() : null);
        ConsultancyDeliverable saved = deliverableRepository.save(d);
        // Spec: REJECTED → IN_PROGRESS automatically for rework
        DeliverableLifecycle.assertTransition(saved.getStatus(), DeliverableStatus.IN_PROGRESS);
        saved.setStatus(DeliverableStatus.IN_PROGRESS);
        saved = deliverableRepository.save(saved);
        auditService.log("CONSULTANCY_DELIVERABLE_REJECTED", MODULE, "ConsultancyDeliverable",
                saved.getId(), DeliverableStatus.SUBMITTED.name(), DeliverableStatus.IN_PROGRESS.name());
        return toDeliverableResponse(saved);
    }

    // ─── Contracts ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ContractResponse getContractByEngagement(Long engagementId) {
        ConsultancyEngagement e = requireEngagement(engagementId);
        assertCanAccessEngagement(e);
        return contractRepository.findByEngagementId(engagementId)
                .map(this::toContractResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found for engagement"));
    }

    @Transactional(readOnly = true)
    public ContractResponse getContract(Long id) {
        ConsultancyContract c = requireContract(id);
        assertCanAccessEngagement(c.getEngagement());
        return toContractResponse(c);
    }

    @Transactional
    public ContractResponse createContract(ContractRequest req) {
        ConsultancyEngagement e = requireEngagement(req.getEngagementId());
        assertCanAccessEngagement(e);
        if (contractRepository.existsByEngagementId(e.getId())) {
            throw new BusinessException("Engagement already has a contract");
        }
        ConsultancyContract c = new ConsultancyContract();
        c.setContractNumber(nextContractNumber());
        c.setEngagement(e);
        c.setCustomer(e.getCustomer());
        c.setTitle(req.getTitle().trim());
        c.setScope(req.getScope());
        c.setTerms(req.getTerms());
        c.setStartDate(req.getStartDate());
        c.setEndDate(req.getEndDate());
        c.setContractValue(nvl(req.getContractValue()).setScale(2, RoundingMode.HALF_UP));
        c.setCurrency(req.getCurrency() != null && !req.getCurrency().isBlank() ? req.getCurrency() : "ETB");
        c.setStatus(ContractStatus.DRAFT);
        c.setNotes(req.getNotes());
        c.setCreatedBy(currentUser());
        ConsultancyContract saved = contractRepository.save(c);
        auditService.log("CONSULTANCY_CONTRACT_CREATED", MODULE, "ConsultancyContract",
                saved.getId(), null, saved.getContractNumber());
        return toContractResponse(saved);
    }

    @Transactional
    public ContractResponse updateContract(Long id, ContractRequest req) {
        ConsultancyContract c = requireContract(id);
        assertCanAccessEngagement(c.getEngagement());
        if (!ContractLifecycle.isEditable(c.getStatus())) {
            throw new BusinessException("Contract cannot be edited in status " + c.getStatus());
        }
        if (req.getTitle() != null && !req.getTitle().isBlank()) c.setTitle(req.getTitle().trim());
        if (req.getScope() != null) c.setScope(req.getScope());
        if (req.getTerms() != null) c.setTerms(req.getTerms());
        if (req.getStartDate() != null) c.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) c.setEndDate(req.getEndDate());
        if (req.getContractValue() != null) {
            c.setContractValue(req.getContractValue().setScale(2, RoundingMode.HALF_UP));
        }
        if (req.getCurrency() != null) c.setCurrency(req.getCurrency());
        if (req.getNotes() != null) c.setNotes(req.getNotes());
        ConsultancyContract saved = contractRepository.save(c);
        auditService.log("CONSULTANCY_CONTRACT_UPDATED", MODULE, "ConsultancyContract",
                saved.getId(), null, saved.getContractNumber());
        return toContractResponse(saved);
    }

    @Transactional
    public ContractResponse activateContract(Long id) {
        ConsultancyContract c = requireContract(id);
        assertCanAccessEngagement(c.getEngagement());
        if (c.getStatus() == ContractStatus.DRAFT) {
            ContractLifecycle.assertTransition(c.getStatus(), ContractStatus.PENDING_SIGNATURE);
            c.setStatus(ContractStatus.PENDING_SIGNATURE);
            contractRepository.save(c);
        }
        ContractLifecycle.assertTransition(c.getStatus(), ContractStatus.ACTIVE);
        c.setStatus(ContractStatus.ACTIVE);
        c.setSignedDate(LocalDate.now());
        ConsultancyContract saved = contractRepository.save(c);
        auditService.log("CONSULTANCY_CONTRACT_ACTIVATED", MODULE, "ConsultancyContract",
                saved.getId(), null, saved.getContractNumber());
        notificationService.createRoleNotification(
                "Consultancy contract activated",
                saved.getContractNumber() + " — " + saved.getTitle(),
                Role.GENERAL_MANAGER);
        return toContractResponse(saved);
    }

    // ─── Invoicing / payments ────────────────────────────────────

    @Transactional
    public EngagementResponse createEngagementInvoice(Long engagementId) {
        ConsultancyEngagement e = engagementRepository.findByIdForUpdate(engagementId)
                .orElseThrow(() -> new ResourceNotFoundException("Engagement not found"));
        assertCanAccessEngagement(e);
        if (e.getStatus() == EngagementStatus.CANCELLED) {
            throw new BusinessException("Cannot invoice a cancelled engagement");
        }
        if (e.getStatus() != EngagementStatus.ACTIVE && e.getStatus() != EngagementStatus.COMPLETED) {
            throw new BusinessException("Invoice requires an ACTIVE or COMPLETED engagement");
        }
        if (e.getInvoice() != null) {
            return toEngagementResponse(e, true);
        }
        Optional<Invoice> existing = invoiceRepository.findBySourceModuleAndSourceTypeAndSourceId(
                SOURCE_MODULE, FinanceSourceTypes.REF_CONSULTANCY_ENGAGEMENT, e.getId());
        if (existing.isPresent()) {
            e.setInvoice(existing.get());
            engagementRepository.save(e);
            return toEngagementResponse(e, true);
        }
        createEngagementInvoiceAndPostRevenue(e);
        engagementRepository.save(e);
        auditService.log("CONSULTANCY_INVOICE_CREATED", MODULE, "ConsultancyEngagement",
                e.getId(), null, e.getInvoice() != null ? e.getInvoice().getInvoiceNumber() : null);
        notificationService.createRoleNotification(
                "Consultancy invoice issued",
                "Invoice for " + e.getEngagementNumber() + " — " + e.getAgreedAmount() + " ETB",
                Role.FINANCE);
        return toEngagementResponse(e, true);
    }

    @Transactional
    public DeliverableResponse billMilestone(Long deliverableId) {
        ConsultancyDeliverable d = requireDeliverable(deliverableId);
        ConsultancyEngagement e = d.getEngagement();
        assertCanAccessEngagement(e);
        if (e.getStatus() == EngagementStatus.CANCELLED) {
            throw new BusinessException("Cannot invoice a cancelled engagement");
        }
        if (d.getStatus() != DeliverableStatus.APPROVED) {
            throw new BusinessException("Milestone invoice requires an approved deliverable");
        }
        if (!d.isBillable() || nvl(d.getBillableAmount()).compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Deliverable is not billable");
        }
        if (d.getInvoice() != null) {
            return toDeliverableResponse(d);
        }
        Optional<Invoice> existing = invoiceRepository.findBySourceModuleAndSourceTypeAndSourceId(
                SOURCE_MODULE, FinanceSourceTypes.REF_CONSULTANCY_MILESTONE, d.getId());
        if (existing.isPresent()) {
            d.setInvoice(existing.get());
            deliverableRepository.save(d);
            return toDeliverableResponse(d);
        }

        BigDecimal amount = nvl(d.getBillableAmount()).setScale(2, RoundingMode.HALF_UP);
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(nextInvoiceNumber());
        invoice.setInvoiceType(InvoiceType.CONSULTANCY);
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setClientId(e.getCustomer().getId());
        invoice.setClientName(e.getCustomer().getClientName());
        invoice.setSubtotal(amount);
        invoice.setDiscountAmount(ZERO);
        invoice.setVatAmount(ZERO);
        invoice.setTotalAmount(amount);
        invoice.setPaidAmount(ZERO);
        invoice.setBusinessUnit(BusinessUnit.CONSULTING);
        invoice.setSourceModule(SOURCE_MODULE);
        invoice.setSourceType(FinanceSourceTypes.REF_CONSULTANCY_MILESTONE);
        invoice.setSourceId(d.getId());
        invoice.setDueDate(LocalDate.now().plusDays(14));

        InvoiceItem item = new InvoiceItem();
        item.setDescription("Milestone: " + d.getTitle() + " / " + e.getEngagementNumber());
        item.setQuantity(1.0);
        item.setUnitPrice(amount);
        item.setLineTotal(amount);
        item.setAccountCode(AccountCode.REV_CONSULTING);
        invoice.addItem(item);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        d.setInvoice(savedInvoice);
        deliverableRepository.save(d);

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            financeService.postTransaction(
                    TransactionType.INCOME, amount, BusinessUnit.CONSULTING,
                    AccountCode.ASSET_RECEIVABLE.getCode(), AccountCode.REV_CONSULTING.getCode(),
                    "Consultancy milestone invoice " + savedInvoice.getInvoiceNumber(),
                    SOURCE_MODULE, FinanceSourceTypes.REF_INVOICE_REVENUE, savedInvoice.getId());
        }

        e.setInvoicedAmount(nvl(e.getInvoicedAmount()).add(amount));
        refreshEngagementOutstanding(e);
        engagementRepository.save(e);

        auditService.log("CONSULTANCY_MILESTONE_INVOICE_CREATED", MODULE, "ConsultancyDeliverable",
                d.getId(), null, savedInvoice.getInvoiceNumber());
        notificationService.createRoleNotification(
                "Consultancy milestone invoiced",
                savedInvoice.getInvoiceNumber() + " — " + amount + " ETB",
                Role.FINANCE);
        return toDeliverableResponse(d);
    }

    @Transactional
    public EngagementResponse recordPayment(Long engagementId, PaymentRequest req) {
        ConsultancyEngagement e = requireEngagement(engagementId);
        assertCanAccessEngagement(e);
        if (e.getStatus() == EngagementStatus.CANCELLED) {
            throw new BusinessException("Cannot record payment on cancelled engagement");
        }
        if (e.getInvoice() == null) {
            throw new BusinessException("Cannot record payment before invoice exists");
        }
        if (req.getPaymentMethod() == PaymentMethod.CREDIT) {
            throw new BusinessException("CREDIT is not a receipt method. Use CASH, BANK_TRANSFER, or MOBILE_MONEY");
        }
        BigDecimal amount = req.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be greater than zero");
        }
        if (amount.compareTo(nvl(e.getOutstandingAmount())) > 0) {
            throw new BusinessException("Payment exceeds outstanding amount of " + e.getOutstandingAmount());
        }

        String debit = req.getPaymentMethod() == PaymentMethod.CASH
                ? AccountCode.ASSET_CASH.getCode()
                : AccountCode.ASSET_BANK.getCode();

        ConsultancyPayment payment = new ConsultancyPayment();
        payment.setPaymentNumber(nextPaymentNumber());
        payment.setEngagement(e);
        payment.setInvoice(e.getInvoice());
        payment.setAmount(amount);
        payment.setCurrency(e.getCurrency() != null ? e.getCurrency() : "ETB");
        payment.setPaymentDate(req.getPaymentDate() != null ? req.getPaymentDate() : LocalDate.now());
        payment.setMethod(req.getPaymentMethod().name());
        payment.setNotes(req.getNotes());
        payment.setRecordedBy(currentUser());
        payment = paymentRepository.save(payment);

        financeService.postTransaction(
                TransactionType.TRANSFER, amount, BusinessUnit.CONSULTING,
                debit, AccountCode.ASSET_RECEIVABLE.getCode(),
                "Consultancy payment " + e.getEngagementNumber() + " (" + req.getPaymentMethod() + ")",
                SOURCE_MODULE, FinanceSourceTypes.REF_PAYMENT, payment.getId(),
                req.getPaymentMethod(), payment.getPaymentDate());

        BigDecimal newPaid = nvl(e.getPaidAmount()).add(amount);
        e.setPaidAmount(newPaid);
        refreshEngagementOutstanding(e);

        Invoice invoice = e.getInvoice();
        invoice.setPaidAmount(nvl(invoice.getPaidAmount()).add(amount));
        BigDecimal invOutstanding = nvl(invoice.getTotalAmount()).subtract(nvl(invoice.getPaidAmount())).max(ZERO);
        invoice.setStatus(invOutstanding.compareTo(BigDecimal.ZERO) == 0
                ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        invoiceRepository.save(invoice);

        ConsultancyEngagement saved = engagementRepository.save(e);
        auditService.log("CONSULTANCY_PAYMENT_CREATED", MODULE, "ConsultancyEngagement",
                saved.getId(), amount.toPlainString(), saved.getPaymentStatus().name());
        notificationService.createRoleNotification(
                "Consultancy payment received",
                saved.getEngagementNumber() + " paid " + amount.toPlainString() + " ETB",
                Role.FINANCE);
        notificationService.createRoleNotification(
                "Consultancy payment received",
                saved.getEngagementNumber() + " paid " + amount.toPlainString() + " ETB",
                Role.CONSULTANT);
        return toEngagementResponse(saved, true);
    }

    // ─── Client history ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> clientHistory(Long clientId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("CRM client not found"));
        List<Map<String, Object>> events = new ArrayList<>();
        for (ConsultancyRequest r : requestRepository.findByCustomerIdOrderByCreatedAtDesc(clientId)) {
            events.add(historyEvent("CONSULTANCY_REQUEST", r.getCreatedAt(), r.getId(),
                    r.getRequestNumber() + " · " + r.getSubject() + " · " + r.getStatus()));
        }
        for (ConsultancyProposal p : proposalRepository.findByCustomerIdOrderByCreatedAtDesc(clientId)) {
            events.add(historyEvent("CONSULTANCY_PROPOSAL", p.getCreatedAt(), p.getId(),
                    p.getProposalNumber() + " · " + p.getTitle() + " · " + p.getStatus()));
        }
        for (ConsultancyEngagement e : engagementRepository.findByCustomerIdOrderByCreatedAtDesc(clientId)) {
            events.add(historyEvent("CONSULTANCY_ENGAGEMENT", e.getCreatedAt(), e.getId(),
                    e.getEngagementNumber() + " · " + e.getTitle() + " · " + e.getStatus()));
            if (e.getInvoice() != null) {
                events.add(historyEvent("CONSULTANCY_INVOICE", e.getInvoice().getCreatedAt(), e.getId(),
                        e.getInvoice().getInvoiceNumber() + " · " + e.getInvoicedAmount() + " ETB"));
            }
            for (ConsultancyPayment pay : paymentRepository.findByEngagementIdOrderByPaymentDateAsc(e.getId())) {
                events.add(historyEvent("CONSULTANCY_PAYMENT", pay.getCreatedAt(), e.getId(),
                        pay.getAmount() + " ETB via " + pay.getMethod()));
            }
            if (e.getCompletedAt() != null) {
                events.add(historyEvent("CONSULTANCY_COMPLETION", e.getCompletedAt(), e.getId(),
                        "Completed " + e.getTitle()));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clientId", clientId);
        result.put("events", events);
        return result;
    }

    // ─── Finance helpers ─────────────────────────────────────────

    private void createEngagementInvoiceAndPostRevenue(ConsultancyEngagement e) {
        BigDecimal amount = nvl(e.getAgreedAmount()).setScale(2, RoundingMode.HALF_UP);
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(nextInvoiceNumber());
        invoice.setInvoiceType(InvoiceType.CONSULTANCY);
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setClientId(e.getCustomer().getId());
        invoice.setClientName(e.getCustomer().getClientName());
        invoice.setSubtotal(amount);
        invoice.setDiscountAmount(ZERO);
        invoice.setVatAmount(ZERO);
        invoice.setTotalAmount(amount);
        invoice.setPaidAmount(ZERO);
        invoice.setBusinessUnit(BusinessUnit.CONSULTING);
        invoice.setSourceModule(SOURCE_MODULE);
        invoice.setSourceType(FinanceSourceTypes.REF_CONSULTANCY_ENGAGEMENT);
        invoice.setSourceId(e.getId());
        invoice.setDueDate(LocalDate.now().plusDays(14));

        InvoiceItem item = new InvoiceItem();
        item.setDescription("Consultancy: " + e.getTitle() + " / " + e.getEngagementNumber());
        item.setQuantity(1.0);
        item.setUnitPrice(amount);
        item.setLineTotal(amount);
        item.setAccountCode(AccountCode.REV_CONSULTING);
        invoice.addItem(item);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        e.setInvoice(savedInvoice);
        e.setInvoicedAmount(amount);
        refreshEngagementOutstanding(e);

        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            financeService.postTransaction(
                    TransactionType.INCOME, amount, BusinessUnit.CONSULTING,
                    AccountCode.ASSET_RECEIVABLE.getCode(), AccountCode.REV_CONSULTING.getCode(),
                    "Consultancy invoice " + savedInvoice.getInvoiceNumber(),
                    SOURCE_MODULE, FinanceSourceTypes.REF_INVOICE_REVENUE, savedInvoice.getId());
        }
    }

    private void refreshEngagementOutstanding(ConsultancyEngagement e) {
        BigDecimal invoiced = nvl(e.getInvoicedAmount());
        BigDecimal paid = nvl(e.getPaidAmount());
        BigDecimal outstanding = invoiced.subtract(paid).max(ZERO);
        e.setOutstandingAmount(outstanding);
        if (invoiced.compareTo(BigDecimal.ZERO) == 0) {
            e.setPaymentStatus(ConsultancyPaymentStatus.UNPAID);
        } else if (outstanding.compareTo(BigDecimal.ZERO) == 0) {
            e.setPaymentStatus(ConsultancyPaymentStatus.PAID);
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            e.setPaymentStatus(ConsultancyPaymentStatus.PARTIALLY_PAID);
        } else {
            e.setPaymentStatus(ConsultancyPaymentStatus.UNPAID);
        }
    }

    private ConsultancyEngagement buildEngagementEntity(EngagementRequest req) {
        CrmClient customer = requireClient(req.getCustomerId());
        ConsultancyEngagement e = new ConsultancyEngagement();
        e.setEngagementNumber(nextEngagementNumber());
        e.setCustomer(customer);
        applyEngagementFields(e, req);
        e.setInvoicedAmount(ZERO);
        e.setPaidAmount(ZERO);
        e.setOutstandingAmount(ZERO);
        e.setPaymentStatus(ConsultancyPaymentStatus.UNPAID);
        e.setCreatedBy(currentUser());
        return e;
    }

    private void applyEngagementFields(ConsultancyEngagement e, EngagementRequest req) {
        if (req.getServiceId() != null) e.setService(requireService(req.getServiceId()));
        if (req.getProposalId() != null) e.setProposal(requireProposal(req.getProposalId()));
        if (req.getRequestId() != null) e.setRequest(requireRequest(req.getRequestId()));
        if (req.getTitle() != null && !req.getTitle().isBlank()) e.setTitle(req.getTitle().trim());
        if (req.getDescription() != null) e.setDescription(req.getDescription());
        if (req.getObjectives() != null) e.setObjectives(req.getObjectives());
        if (req.getScope() != null) e.setScope(req.getScope());
        if (req.getStartDate() != null) e.setStartDate(req.getStartDate());
        if (req.getExpectedEndDate() != null) e.setExpectedEndDate(req.getExpectedEndDate());
        if (req.getLocation() != null) e.setLocation(req.getLocation());
        if (req.getPriority() != null) e.setPriority(req.getPriority());
        if (req.getLeadConsultantId() != null) e.setLeadConsultant(requireUser(req.getLeadConsultantId()));
        if (req.getEstimatedAmount() != null) {
            e.setEstimatedAmount(req.getEstimatedAmount().setScale(2, RoundingMode.HALF_UP));
        }
        if (req.getAgreedAmount() != null) {
            e.setAgreedAmount(req.getAgreedAmount().setScale(2, RoundingMode.HALF_UP));
        }
        if (req.getCurrency() != null && !req.getCurrency().isBlank()) e.setCurrency(req.getCurrency());
        if (req.getBillingType() != null) e.setBillingType(req.getBillingType());
        if (req.getNotes() != null) e.setNotes(req.getNotes());
        if (e.getTitle() == null) {
            throw new BusinessException("Engagement title is required");
        }
        if (e.getAgreedAmount() == null) {
            e.setAgreedAmount(ZERO);
        }
        if (e.getCurrency() == null) e.setCurrency("ETB");
        if (e.getBillingType() == null) e.setBillingType(BillingType.FIXED_PRICE);
        if (e.getPriority() == null) e.setPriority(RequestPriority.MEDIUM);
    }

    private void applyService(ConsultancyServiceCatalog s, ServiceCatalogRequest req, String code) {
        s.setServiceCode(code);
        s.setName(req.getName().trim());
        s.setDescription(req.getDescription());
        s.setCategory(req.getCategory() != null ? req.getCategory() : ServiceCategory.OTHER);
        s.setDefaultDurationDays(req.getDefaultDurationDays());
        s.setDefaultPrice(req.getDefaultPrice().setScale(2, RoundingMode.HALF_UP));
        s.setCurrency(req.getCurrency() != null && !req.getCurrency().isBlank() ? req.getCurrency() : "ETB");
        if (req.getTaxApplicable() != null) s.setTaxApplicable(req.getTaxApplicable());
        if (req.getActive() != null) s.setActive(req.getActive());
    }

    // ─── Mapping ─────────────────────────────────────────────────

    private ServiceCatalogResponse toServiceResponse(ConsultancyServiceCatalog s) {
        return ServiceCatalogResponse.builder()
                .id(s.getId())
                .serviceCode(s.getServiceCode())
                .name(s.getName())
                .description(s.getDescription())
                .category(s.getCategory())
                .defaultDurationDays(s.getDefaultDurationDays())
                .defaultPrice(s.getDefaultPrice())
                .currency(s.getCurrency())
                .taxApplicable(s.isTaxApplicable())
                .active(s.isActive())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private RequestResponse toRequestResponse(ConsultancyRequest r) {
        return RequestResponse.builder()
                .id(r.getId())
                .requestNumber(r.getRequestNumber())
                .customerId(r.getCustomer().getId())
                .customerName(r.getCustomer().getClientName())
                .subject(r.getSubject())
                .description(r.getDescription())
                .serviceId(r.getRequestedService() != null ? r.getRequestedService().getId() : null)
                .serviceName(r.getRequestedService() != null ? r.getRequestedService().getName() : null)
                .requestedDate(r.getRequestedDate())
                .preferredLocation(r.getPreferredLocation())
                .priority(r.getPriority())
                .assignedConsultantId(r.getAssignedConsultant() != null ? r.getAssignedConsultant().getId() : null)
                .assignedConsultantName(r.getAssignedConsultant() != null ? r.getAssignedConsultant().getFullName() : null)
                .status(r.getStatus())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ProposalResponse toProposalResponse(ConsultancyProposal p) {
        return ProposalResponse.builder()
                .id(p.getId())
                .proposalNumber(p.getProposalNumber())
                .customerId(p.getCustomer().getId())
                .customerName(p.getCustomer().getClientName())
                .requestId(p.getRequest() != null ? p.getRequest().getId() : null)
                .requestNumber(p.getRequest() != null ? p.getRequest().getRequestNumber() : null)
                .serviceId(p.getService() != null ? p.getService().getId() : null)
                .serviceName(p.getService() != null ? p.getService().getName() : null)
                .title(p.getTitle())
                .scope(p.getScope())
                .proposedAmount(p.getProposedAmount())
                .currency(p.getCurrency())
                .validUntil(p.getValidUntil())
                .status(p.getStatus())
                .notes(p.getNotes())
                .convertedEngagementId(p.getConvertedEngagementId())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private EngagementResponse toEngagementResponse(ConsultancyEngagement e, boolean detail) {
        BigDecimal revenue = ZERO;
        if (detail) {
            revenue = financeService.getAllTransactions().stream()
                    .filter(t -> SOURCE_MODULE.equals(t.getSourceModule()))
                    .filter(t -> t.getTransactionType() == TransactionType.INCOME)
                    .filter(t -> {
                        if (e.getInvoice() != null && e.getInvoice().getId().equals(t.getReferenceId())
                                && FinanceSourceTypes.REF_INVOICE_REVENUE.equals(t.getReferenceType())) {
                            return true;
                        }
                        return false;
                    })
                    .map(FinanceTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (revenue.compareTo(BigDecimal.ZERO) == 0) {
                revenue = nvl(e.getInvoicedAmount());
            }
        }
        return EngagementResponse.builder()
                .id(e.getId())
                .engagementNumber(e.getEngagementNumber())
                .customerId(e.getCustomer().getId())
                .customerName(e.getCustomer().getClientName())
                .serviceId(e.getService() != null ? e.getService().getId() : null)
                .serviceName(e.getService() != null ? e.getService().getName() : null)
                .proposalId(e.getProposal() != null ? e.getProposal().getId() : null)
                .requestId(e.getRequest() != null ? e.getRequest().getId() : null)
                .title(e.getTitle())
                .description(e.getDescription())
                .objectives(e.getObjectives())
                .scope(e.getScope())
                .startDate(e.getStartDate())
                .expectedEndDate(e.getExpectedEndDate())
                .actualEndDate(e.getActualEndDate())
                .location(e.getLocation())
                .status(e.getStatus())
                .priority(e.getPriority())
                .leadConsultantId(e.getLeadConsultant() != null ? e.getLeadConsultant().getId() : null)
                .leadConsultantName(e.getLeadConsultant() != null ? e.getLeadConsultant().getFullName() : null)
                .estimatedAmount(e.getEstimatedAmount())
                .agreedAmount(e.getAgreedAmount())
                .currency(e.getCurrency())
                .billingType(e.getBillingType())
                .invoicedAmount(e.getInvoicedAmount())
                .paidAmount(e.getPaidAmount())
                .outstandingAmount(e.getOutstandingAmount())
                .paymentStatus(e.getPaymentStatus())
                .invoiceId(e.getInvoice() != null ? e.getInvoice().getId() : null)
                .invoiceNumber(e.getInvoice() != null ? e.getInvoice().getInvoiceNumber() : null)
                .notes(e.getNotes())
                .completedAt(e.getCompletedAt())
                .completionNotes(e.getCompletionNotes())
                .createdAt(e.getCreatedAt())
                .revenueRecognized(revenue)
                .profitNote(PROFIT_NOTE)
                .build();
    }

    private ActivityResponse toActivityResponse(ConsultancyActivity a) {
        return ActivityResponse.builder()
                .id(a.getId())
                .activityNumber(a.getActivityNumber())
                .engagementId(a.getEngagement().getId())
                .engagementNumber(a.getEngagement().getEngagementNumber())
                .customerId(a.getCustomer().getId())
                .consultantId(a.getConsultant() != null ? a.getConsultant().getId() : null)
                .consultantName(a.getConsultant() != null ? a.getConsultant().getFullName() : null)
                .activityType(a.getActivityType())
                .activityDate(a.getActivityDate())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .durationMinutes(a.getDurationMinutes())
                .location(a.getLocation())
                .description(a.getDescription())
                .findings(a.getFindings())
                .recommendations(a.getRecommendations())
                .customerNotes(a.getCustomerNotes())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private DeliverableResponse toDeliverableResponse(ConsultancyDeliverable d) {
        return DeliverableResponse.builder()
                .id(d.getId())
                .deliverableNumber(d.getDeliverableNumber())
                .engagementId(d.getEngagement().getId())
                .engagementNumber(d.getEngagement().getEngagementNumber())
                .title(d.getTitle())
                .description(d.getDescription())
                .dueDate(d.getDueDate())
                .submittedDate(d.getSubmittedDate())
                .approvedDate(d.getApprovedDate())
                .status(d.getStatus())
                .assignedToId(d.getAssignedTo() != null ? d.getAssignedTo().getId() : null)
                .assignedToName(d.getAssignedTo() != null ? d.getAssignedTo().getFullName() : null)
                .billableAmount(d.getBillableAmount())
                .billable(d.isBillable())
                .invoiceId(d.getInvoice() != null ? d.getInvoice().getId() : null)
                .invoiceNumber(d.getInvoice() != null ? d.getInvoice().getInvoiceNumber() : null)
                .notes(d.getNotes())
                .rejectionReason(d.getRejectionReason())
                .createdAt(d.getCreatedAt())
                .build();
    }

    private ContractResponse toContractResponse(ConsultancyContract c) {
        return ContractResponse.builder()
                .id(c.getId())
                .contractNumber(c.getContractNumber())
                .engagementId(c.getEngagement().getId())
                .engagementNumber(c.getEngagement().getEngagementNumber())
                .customerId(c.getCustomer().getId())
                .customerName(c.getCustomer().getClientName())
                .title(c.getTitle())
                .scope(c.getScope())
                .terms(c.getTerms())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .contractValue(c.getContractValue())
                .currency(c.getCurrency())
                .status(c.getStatus())
                .signedDate(c.getSignedDate())
                .notes(c.getNotes())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private Map<String, Object> historyEvent(String type, Object at, Long entityId, String summary) {
        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("type", type);
        ev.put("at", at);
        ev.put("entityId", entityId);
        ev.put("summary", summary);
        return ev;
    }

    // ─── Require helpers ─────────────────────────────────────────

    private ConsultancyServiceCatalog requireService(Long id) {
        return serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultancy service not found"));
    }

    private ConsultancyRequest requireRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultancy request not found"));
    }

    private ConsultancyProposal requireProposal(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultancy proposal not found"));
    }

    private ConsultancyEngagement requireEngagement(Long id) {
        return engagementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Engagement not found"));
    }

    private ConsultancyActivity requireActivity(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
    }

    private ConsultancyDeliverable requireDeliverable(Long id) {
        return deliverableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deliverable not found"));
    }

    private ConsultancyContract requireContract(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
    }

    private CrmClient requireClient(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CRM client not found"));
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // ─── Number generators ───────────────────────────────────────

    private String nextServiceCode() {
        return "CSV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"))
                + "-" + String.format("%03d", serviceCatalogRepository.count() + 1);
    }

    private String nextRequestNumber() {
        return "REQ-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", requestRepository.count() + 1);
    }

    private String nextProposalNumber() {
        return "PROP-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", proposalRepository.count() + 1);
    }

    private String nextEngagementNumber() {
        return "ENG-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", engagementRepository.count() + 1);
    }

    private String nextContractNumber() {
        return "CON-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", contractRepository.count() + 1);
    }

    private String nextActivityNumber() {
        return "ACT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", activityRepository.count() + 1);
    }

    private String nextDeliverableNumber() {
        return "DEL-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", deliverableRepository.count() + 1);
    }

    private String nextPaymentNumber() {
        return "CPAY-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%04d", paymentRepository.count() + 1);
    }

    private String nextInvoiceNumber() {
        return "CSINV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private User currentUser() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : ZERO;
    }
}
