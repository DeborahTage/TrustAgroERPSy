package com.trustagro.crm.service;

import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.crm.dto.*;
import com.trustagro.crm.entity.CrmClient;
import com.trustagro.crm.entity.FarmVisit;
import com.trustagro.crm.repository.CrmClientRepository;
import com.trustagro.crm.repository.FarmVisitRepository;
import com.trustagro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrmService {

    private final CrmClientRepository clientRepository;
    private final FarmVisitRepository visitRepository;
    private final UserRepository userRepository;

    public List<CrmClientResponse> getAllClients() {
        return clientRepository.findAll().stream().map(this::toClientResponse).collect(Collectors.toList());
    }

    public CrmClientResponse getClientById(Long id) {
        return toClientResponse(findClientById(id));
    }

    public CrmClient getClientEntity(Long id) {
        return findClientById(id);
    }

    public List<CrmClientResponse> getLeads() {
        return clientRepository.findAll().stream()
                .filter(c -> c.getStatus() == com.trustagro.crm.entity.ClientStatus.LEAD)
                .map(this::toClientResponse).collect(Collectors.toList());
    }

    public Map<String, Long> getPipelineSummary() {
        return clientRepository.findAll().stream()
                .collect(Collectors.groupingBy(c -> c.getStatus().name(), Collectors.counting()));
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        var all = clientRepository.findAll();
        stats.put("totalClients", all.size());
        stats.put("leads", all.stream().filter(c -> c.getStatus() == com.trustagro.crm.entity.ClientStatus.LEAD).count());
        stats.put("activeClients", all.stream().filter(c -> c.getStatus() == com.trustagro.crm.entity.ClientStatus.ACTIVE).count());
        stats.put("totalVisits", visitRepository.count());
        stats.put("dueFollowUps", visitRepository.findByNextFollowUpDateLessThanEqual(LocalDate.now()).size());
        return stats;
    }

    public CrmClientResponse createClient(CrmClientRequest req) {
        CrmClient client = new CrmClient();
        mapClientRequest(req, client);
        return toClientResponse(clientRepository.save(client));
    }

    public CrmClientResponse updateClient(Long id, CrmClientRequest req) {
        CrmClient client = findClientById(id);
        mapClientRequest(req, client);
        return toClientResponse(clientRepository.save(client));
    }

    public List<FarmVisitResponse> getAllVisits() {
        return visitRepository.findAll().stream().map(this::toVisitResponse).collect(Collectors.toList());
    }

    public List<FarmVisitResponse> getVisitsByClient(Long clientId) {
        return visitRepository.findByClientId(clientId).stream().map(this::toVisitResponse).collect(Collectors.toList());
    }

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
        return toVisitResponse(visitRepository.save(visit));
    }

    public List<FarmVisitResponse> getDueFollowUps() {
        return visitRepository.findByNextFollowUpDateLessThanEqual(LocalDate.now())
                .stream().map(this::toVisitResponse).collect(Collectors.toList());
    }

    private void mapClientRequest(CrmClientRequest req, CrmClient client) {
        client.setClientName(req.getClientName());
        client.setPhone(req.getPhone());
        client.setLocation(req.getLocation());
        client.setFarmType(req.getFarmType());
        client.setFarmSize(req.getFarmSize());
        client.setNumberOfBirds(req.getNumberOfBirds());
        if (req.getStatus() != null) client.setStatus(req.getStatus());
        if (req.getAssignedExtensionWorkerId() != null)
            userRepository.findById(req.getAssignedExtensionWorkerId()).ifPresent(client::setAssignedExtensionWorker);
    }

    private CrmClient findClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
    }

    private CrmClientResponse toClientResponse(CrmClient c) {
        CrmClientResponse r = new CrmClientResponse();
        r.setId(c.getId());
        r.setClientName(c.getClientName());
        r.setPhone(c.getPhone());
        r.setLocation(c.getLocation());
        r.setFarmType(c.getFarmType());
        r.setFarmSize(c.getFarmSize());
        r.setNumberOfBirds(c.getNumberOfBirds());
        r.setStatus(c.getStatus());
        r.setCreatedAt(c.getCreatedAt());
        r.setUpdatedAt(c.getUpdatedAt());
        if (c.getAssignedExtensionWorker() != null) {
            r.setAssignedExtensionWorkerId(c.getAssignedExtensionWorker().getId());
            r.setAssignedExtensionWorkerName(c.getAssignedExtensionWorker().getFullName());
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
