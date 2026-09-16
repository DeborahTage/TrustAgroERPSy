package com.trustagro.crm.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.crm.entity.OpportunityStage;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Opportunity stage lifecycle.
 * <pre>
 * PROSPECTING → QUALIFICATION → PROPOSAL → NEGOTIATION → WON
 *      └────────────┴───────────┴──────────┴────────────→ LOST
 * </pre>
 */
public final class OpportunityLifecycle {

    private static final Map<OpportunityStage, Set<OpportunityStage>> ALLOWED = new EnumMap<>(OpportunityStage.class);

    static {
        ALLOWED.put(OpportunityStage.PROSPECTING, EnumSet.of(
                OpportunityStage.QUALIFICATION, OpportunityStage.PROPOSAL, OpportunityStage.LOST));
        ALLOWED.put(OpportunityStage.QUALIFICATION, EnumSet.of(
                OpportunityStage.PROPOSAL, OpportunityStage.NEGOTIATION, OpportunityStage.LOST));
        ALLOWED.put(OpportunityStage.PROPOSAL, EnumSet.of(
                OpportunityStage.NEGOTIATION, OpportunityStage.WON, OpportunityStage.LOST));
        ALLOWED.put(OpportunityStage.NEGOTIATION, EnumSet.of(
                OpportunityStage.WON, OpportunityStage.LOST, OpportunityStage.PROPOSAL));
        ALLOWED.put(OpportunityStage.WON, EnumSet.noneOf(OpportunityStage.class));
        ALLOWED.put(OpportunityStage.LOST, EnumSet.noneOf(OpportunityStage.class));
    }

    private OpportunityLifecycle() {}

    public static void assertTransition(OpportunityStage from, OpportunityStage to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid opportunity stage transition: " + from + " → " + to);
        }
    }
}
