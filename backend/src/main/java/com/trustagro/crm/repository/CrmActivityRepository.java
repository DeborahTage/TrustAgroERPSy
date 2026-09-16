package com.trustagro.crm.repository;

import com.trustagro.crm.entity.CrmActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrmActivityRepository extends JpaRepository<CrmActivity, Long> {
    List<CrmActivity> findByClientIdOrderByActivityDateDesc(Long clientId);
    List<CrmActivity> findByLeadIdOrderByActivityDateDesc(Long leadId);
    List<CrmActivity> findByOpportunityIdOrderByActivityDateDesc(Long opportunityId);
    List<CrmActivity> findAllByOrderByActivityDateDesc();
}
