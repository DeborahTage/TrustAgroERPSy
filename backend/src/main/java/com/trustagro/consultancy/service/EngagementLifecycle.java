package com.trustagro.consultancy.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.consultancy.entity.EngagementStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Engagement lifecycle (backend-authoritative).
 * <pre>
 * DRAFT → PROPOSED → APPROVED → ACTIVE → ON_HOLD | COMPLETED | CANCELLED
 *                └────────────┴──────────→ CANCELLED
 * ON_HOLD → ACTIVE
 * </pre>
 */
public final class EngagementLifecycle {

    private static final Map<EngagementStatus, Set<EngagementStatus>> ALLOWED = new EnumMap<>(EngagementStatus.class);

    static {
        ALLOWED.put(EngagementStatus.DRAFT, EnumSet.of(EngagementStatus.PROPOSED, EngagementStatus.CANCELLED));
        ALLOWED.put(EngagementStatus.PROPOSED, EnumSet.of(EngagementStatus.APPROVED, EngagementStatus.CANCELLED));
        ALLOWED.put(EngagementStatus.APPROVED, EnumSet.of(EngagementStatus.ACTIVE, EngagementStatus.CANCELLED));
        ALLOWED.put(EngagementStatus.ACTIVE, EnumSet.of(EngagementStatus.ON_HOLD, EngagementStatus.COMPLETED, EngagementStatus.CANCELLED));
        ALLOWED.put(EngagementStatus.ON_HOLD, EnumSet.of(EngagementStatus.ACTIVE, EngagementStatus.CANCELLED));
        ALLOWED.put(EngagementStatus.COMPLETED, EnumSet.noneOf(EngagementStatus.class));
        ALLOWED.put(EngagementStatus.CANCELLED, EnumSet.noneOf(EngagementStatus.class));
    }

    private EngagementLifecycle() {}

    public static void assertTransition(EngagementStatus from, EngagementStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid engagement status transition: " + from + " → " + to);
        }
    }

    public static boolean isEditable(EngagementStatus status) {
        return status == EngagementStatus.DRAFT || status == EngagementStatus.PROPOSED;
    }

    public static boolean isBillable(EngagementStatus status) {
        return status == EngagementStatus.ACTIVE || status == EngagementStatus.COMPLETED
                || status == EngagementStatus.APPROVED;
    }
}
