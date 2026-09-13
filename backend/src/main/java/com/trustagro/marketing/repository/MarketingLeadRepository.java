package com.trustagro.marketing.repository;

import com.trustagro.marketing.entity.MarketingLead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketingLeadRepository extends JpaRepository<MarketingLead, Long> {
    List<MarketingLead> findByCampaignId(Long campaignId);
    long countByConvertedFalse();
}
