package com.trustagro.crm.repository;

import com.trustagro.crm.entity.CrmOpportunity;
import com.trustagro.crm.entity.OpportunityStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface CrmOpportunityRepository extends JpaRepository<CrmOpportunity, Long> {
    List<CrmOpportunity> findByClientIdOrderByCreatedAtDesc(Long clientId);
    List<CrmOpportunity> findByStageOrderByCreatedAtDesc(OpportunityStage stage);
    List<CrmOpportunity> findAllByOrderByCreatedAtDesc();
    List<CrmOpportunity> findByLeadId(Long leadId);

    long countByStage(OpportunityStage stage);

    @Query("SELECT o.stage, COUNT(o) FROM CrmOpportunity o GROUP BY o.stage")
    List<Object[]> countGroupedByStage();

    @Query("SELECT COALESCE(SUM(o.estimatedValue), 0) FROM CrmOpportunity o " +
           "WHERE o.stage NOT IN (com.trustagro.crm.entity.OpportunityStage.WON, " +
           "com.trustagro.crm.entity.OpportunityStage.LOST)")
    BigDecimal sumOpenEstimatedValue();
}
