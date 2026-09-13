package com.trustagro.marketing.service;

import com.trustagro.common.exception.ResourceNotFoundException;
import com.trustagro.crm.dto.CrmClientRequest;
import com.trustagro.crm.entity.ClientStatus;
import com.trustagro.crm.service.CrmService;
import com.trustagro.marketing.entity.CampaignStatus;
import com.trustagro.marketing.entity.MarketingCampaign;
import com.trustagro.marketing.entity.MarketingLead;
import com.trustagro.marketing.repository.MarketingCampaignRepository;
import com.trustagro.marketing.repository.MarketingLeadRepository;
import com.trustagro.notification.entity.Notification;
import com.trustagro.notification.entity.NotificationType;
import com.trustagro.notification.repository.NotificationRepository;
import com.trustagro.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketingService {

    private final MarketingCampaignRepository campaignRepository;
    private final MarketingLeadRepository leadRepository;
    private final CrmService crmService;
    private final NotificationRepository notificationRepository;

    public Map<String, Object> getDashboard() {
        Map<String, Object> data = new HashMap<>();
        List<MarketingCampaign> campaigns = campaignRepository.findAll();
        data.put("totalCampaigns", campaigns.size());
        data.put("activeCampaigns", campaigns.stream().filter(c -> c.getStatus() == CampaignStatus.ACTIVE).count());
        data.put("totalLeads", leadRepository.count());
        data.put("openLeads", leadRepository.countByConvertedFalse());
        data.put("totalBudget", campaigns.stream().map(c -> c.getBudget() != null ? c.getBudget() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        data.put("totalSpent", campaigns.stream().map(c -> c.getSpent() != null ? c.getSpent() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        long conversions = campaigns.stream().mapToInt(c -> c.getConversions() != null ? c.getConversions() : 0).sum();
        long leads = leadRepository.count();
        data.put("conversionRate", leads > 0 ? BigDecimal.valueOf(conversions * 100.0 / leads).setScale(1, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        return data;
    }

    public List<MarketingCampaign> getCampaigns() {
        return campaignRepository.findAll();
    }

    public MarketingCampaign createCampaign(MarketingCampaign campaign) {
        return campaignRepository.save(campaign);
    }

    public List<MarketingLead> getLeads() {
        return leadRepository.findAll();
    }

    @Transactional
    public MarketingLead createLead(MarketingLead lead) {
        MarketingLead saved = leadRepository.save(lead);
        if (lead.getCampaignId() != null) {
            campaignRepository.findById(lead.getCampaignId()).ifPresent(c -> {
                c.setLeadsGenerated((c.getLeadsGenerated() != null ? c.getLeadsGenerated() : 0) + 1);
                campaignRepository.save(c);
            });
        }
        Notification n = new Notification();
        n.setTitle("New Marketing Lead");
        n.setMessage(saved.getFullName() + " from " + (saved.getSource() != null ? saved.getSource() : "unknown source"));
        n.setType(NotificationType.SYSTEM);
        n.setTargetRole(Role.CRM_MANAGER);
        notificationRepository.save(n);
        return saved;
    }

    @Transactional
    public MarketingLead convertLeadToCrm(Long leadId) {
        MarketingLead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        if (lead.isConverted()) return lead;

        CrmClientRequest req = new CrmClientRequest();
        req.setClientName(lead.getFullName() != null ? lead.getFullName() : "Lead #" + leadId);
        req.setPhone(lead.getPhone());
        req.setLocation(lead.getOrganization());
        req.setStatus(ClientStatus.LEAD);

        var client = crmService.createClient(req);
        lead.setConverted(true);
        lead.setCrmClient(crmService.getClientEntity(client.getId()));

        if (lead.getCampaignId() != null) {
            campaignRepository.findById(lead.getCampaignId()).ifPresent(c -> {
                c.setConversions((c.getConversions() != null ? c.getConversions() : 0) + 1);
                campaignRepository.save(c);
            });
        }

        Notification n = new Notification();
        n.setTitle("Lead Converted to CRM");
        n.setMessage(lead.getFullName() + " added as CRM lead");
        n.setType(NotificationType.SYSTEM);
        n.setTargetRole(Role.CRM_MANAGER);
        notificationRepository.save(n);

        return leadRepository.save(lead);
    }
}
