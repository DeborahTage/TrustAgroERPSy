package com.trustagro.crm.service;

import com.trustagro.audit.service.AuditService;
import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.crm.dto.*;
import com.trustagro.crm.entity.ClientStatus;
import com.trustagro.crm.entity.CrmClient;
import com.trustagro.crm.entity.CrmNumberSequence;
import com.trustagro.crm.entity.FarmVisit;
import com.trustagro.crm.repository.CrmClientRepository;
import com.trustagro.crm.repository.CrmNumberSequenceRepository;
import com.trustagro.crm.repository.FarmVisitRepository;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrmService {

    public static final String MODULE = "CRM";
    private static final String SEQ_CUSTOMER = "CUSTOMER";

    private final CrmClientRepository clientRepository;
    private final FarmVisitRepository visitRepository;
    private final UserRepository userRepository;
    private final CrmNumberSequenceRepository sequenceRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CrmClientResponse> getAllClients() {
        return clientRepository.findAll().stream().map(this::toClientResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CrmClientResponse> getClientsByStatus(ClientStatus status) {
        return clientRepository.findByStatus(status).stream().map(this::toClientResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CrmClientResponse getClientById(Long id) {
        return toClientResponse(findClientById(id));
    }

    public CrmClient getClientEntity(Long id) {
        return findClientById(id);
    }

    /** Legacy: clients still marked LEAD status (not CrmLead pipeline). */
    @Transactional(readOnly = true)
    public List<CrmClientResponse> getClientLeads() {
        return getClientsByStatus(ClientStatus.LEAD);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getPipelineSummary() {
        return clientRepository.findAll().stream()
                .collect(Collectors.groupingBy(c -> c.getStatus().name(), Collectors.counting()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        var all = clientRepository.findAll();
        stats.put("totalClients", all.size());
        stats.put("leads", all.stream().filter(c -> c.getStatus() == ClientStatus.LEAD).count());
        stats.put("activeClients", all.stream().filter(c -> c.getStatus() == ClientStatus.ACTIVE).count());
        stats.put("totalVisits", visitRepository.count());
        stats.put("dueFollowUps", visitRepository.findByNextFollowUpDateLessThanEqual(LocalDate.now()).size());
        return stats;
    }

    @Transactional
    public CrmClientResponse createClient(CrmClientRequest req) {
        CrmClient client = new CrmClient();
        mapClientRequest(req, client);
        if (client.getCustomerCode() == null || client.getCustomerCode().isBlank()) {
            client.setCustomerCode(nextCustomerCode());
        }
        CrmClient saved = clientRepository.save(client);
        auditService.log("CREATE", MODULE, "CrmClient", saved.getId(), null, saved.getCustomerCode());
        return toClientResponse(saved);
    }

    @Transactional
    public CrmClientResponse updateClient(Long id, CrmClientRequest req) {
        CrmClient client = findClientById(id);
        mapClientRequest(req, client);
        CrmClient saved = clientRepository.save(client);
        auditService.log("UPDATE", MODULE, "CrmClient", saved.getId(), null, saved.getCustomerCode());
        return toClientResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FarmVisitResponse> getAllVisits() {
        return visitRepository.findAll().stream().map(this::toVisitResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FarmVisitResponse> getVisitsByClient(Long clientId) {
        return visitRepository.findByClientId(clientId).stream().map(this::toVisitResponse).collect(Collectors.toList());
    }

    @Transactional
    public FarmVisitResponse createVisit(FarmVisitRequest req) {
        CrmClient client = findClientById(req.getClientId());
        FarmVisit visit = new FarmVisit();
        visit.setClient(client);
        visit.setVisitDate(req.getVisitDate() != null ? req.getVisitDate() : LocalDate.now());
        visit.setPurpose(req.getPurpose());
        visit.setObservation(req.getObservation());
        visit.setAdviceGiven(req.getAdviceGiven());
        visit.setNextFollowUpDate(req.getNextFollowUpDate());
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        userRepository.findByEmail(email).ifPresent(visit::setVisitedBy);
        FarmVisit saved = visitRepository.save(visit);
        auditService.log("CREATE", MODULE, "FarmVisit", saved.getId(), null, client.getClientName());
        return toVisitResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FarmVisitResponse> getDueFollowUps() {
        return visitRepository.findByNextFollowUpDateLessThanEqual(LocalDate.now())
                .stream().map(this::toVisitResponse).collect(Collectors.toList());
    }

    String nextCustomerCode() {
        return nextNumber(SEQ_CUSTOMER, "CUS");
    }

    String nextNumber(String sequenceName, String prefix) {
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

    private void mapClientRequest(CrmClientRequest req, CrmClient client) {
        client.setClientName(req.getClientName());
        client.setPhone(req.getPhone());
        client.setEmail(req.getEmail());
        client.setContactPerson(req.getContactPerson());
        client.setAddress(req.getAddress());
        client.setCity(req.getCity());
        client.setRegion(req.getRegion());
        client.setLocation(req.getLocation());
        client.setFarmType(req.getFarmType());
        client.setFarmSize(req.getFarmSize());
        client.setNumberOfChickens(req.getNumberOfChickens());
        if (req.getCustomerType() != null) client.setCustomerType(req.getCustomerType());
        client.setTaxIdentifier(req.getTaxIdentifier());
        client.setNotes(req.getNotes());
        if (req.getStatus() != null) client.setStatus(req.getStatus());
        if (req.getAssignedExtensionWorkerId() != null) {
            userRepository.findById(req.getAssignedExtensionWorkerId()).ifPresent(client::setAssignedExtensionWorker);
        }
        if (req.getAssignedOwnerId() != null) {
            userRepository.findById(req.getAssignedOwnerId()).ifPresent(client::setAssignedOwner);
        }
    }

    private CrmClient findClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
    }

    CrmClientResponse toClientResponse(CrmClient c) {
        CrmClientResponse r = new CrmClientResponse();
        r.setId(c.getId());
        r.setCustomerCode(c.getCustomerCode());
        r.setClientName(c.getClientName());
        r.setPhone(c.getPhone());
        r.setEmail(c.getEmail());
        r.setContactPerson(c.getContactPerson());
        r.setAddress(c.getAddress());
        r.setCity(c.getCity());
        r.setRegion(c.getRegion());
        r.setLocation(c.getLocation());
        r.setFarmType(c.getFarmType());
        r.setFarmSize(c.getFarmSize());
        r.setNumberOfChickens(c.getNumberOfChickens());
        r.setCustomerType(c.getCustomerType());
        r.setTaxIdentifier(c.getTaxIdentifier());
        r.setNotes(c.getNotes());
        r.setStatus(c.getStatus());
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
        if (c.getAssignedExtensionWorker() != null) {
            r.setAssignedExtensionWorkerId(c.getAssignedExtensionWorker().getId());
            r.setAssignedExtensionWorkerName(c.getAssignedExtensionWorker().getFullName());
        }
        if (c.getAssignedOwner() != null) {
            r.setAssignedOwnerId(c.getAssignedOwner().getId());
            r.setAssignedOwnerName(c.getAssignedOwner().getFullName());
        }
        return r;
    }

    private FarmVisitResponse toVisitResponse(FarmVisit v) {
        FarmVisitResponse r = new FarmVisitResponse();
        r.setId(v.getId());
        r.setClientId(v.getClient().getId());
        r.setClientName(v.getClient().getClientName());
        r.setVisitDate(v.getVisitDate());
        r.setPurpose(v.getPurpose());
        r.setObservation(v.getObservation());
        r.setAdviceGiven(v.getAdviceGiven());
        r.setNextFollowUpDate(v.getNextFollowUpDate());
        r.setCreatedAt(v.getCreatedAt());
        if (v.getVisitedBy() != null) r.setVisitedBy(v.getVisitedBy().getFullName());
        return r;
    }
}
