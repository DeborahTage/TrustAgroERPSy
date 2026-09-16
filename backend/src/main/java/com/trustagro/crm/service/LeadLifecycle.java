package com.trustagro.crm.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.crm.entity.CrmLeadStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lead lifecycle.
 * <pre>
 * NEW → CONTACTED → QUALIFIED → CONVERTED
 *   └─────────────┴───────────→ UNQUALIFIED | LOST
 * </pre>
 */
public final class LeadLifecycle {

    private static final Map<CrmLeadStatus, Set<CrmLeadStatus>> ALLOWED = new EnumMap<>(CrmLeadStatus.class);

    static {
        ALLOWED.put(CrmLeadStatus.NEW, EnumSet.of(
                CrmLeadStatus.CONTACTED, CrmLeadStatus.QUALIFIED, CrmLeadStatus.UNQUALIFIED, CrmLeadStatus.LOST));
        ALLOWED.put(CrmLeadStatus.CONTACTED, EnumSet.of(
                CrmLeadStatus.QUALIFIED, CrmLeadStatus.UNQUALIFIED, CrmLeadStatus.LOST));
        ALLOWED.put(CrmLeadStatus.QUALIFIED, EnumSet.of(
                CrmLeadStatus.CONVERTED, CrmLeadStatus.UNQUALIFIED, CrmLeadStatus.LOST));
        ALLOWED.put(CrmLeadStatus.UNQUALIFIED, EnumSet.noneOf(CrmLeadStatus.class));
        ALLOWED.put(CrmLeadStatus.CONVERTED, EnumSet.noneOf(CrmLeadStatus.class));
        ALLOWED.put(CrmLeadStatus.LOST, EnumSet.noneOf(CrmLeadStatus.class));
    }

    private LeadLifecycle() {}

    public static void assertTransition(CrmLeadStatus from, CrmLeadStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid lead status transition: " + from + " → " + to);
        }
    }
}
