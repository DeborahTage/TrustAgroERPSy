package com.trustagro.consultancy.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.consultancy.entity.DeliverableStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Deliverable lifecycle.
 * <pre>
 * PENDING → IN_PROGRESS → SUBMITTED → APPROVED
 *                     └────────────→ REJECTED → IN_PROGRESS
 * CANCELLED from non-terminal states
 * </pre>
 */
public final class DeliverableLifecycle {

    private static final Map<DeliverableStatus, Set<DeliverableStatus>> ALLOWED = new EnumMap<>(DeliverableStatus.class);

    static {
        ALLOWED.put(DeliverableStatus.PENDING, EnumSet.of(DeliverableStatus.IN_PROGRESS, DeliverableStatus.CANCELLED));
        ALLOWED.put(DeliverableStatus.IN_PROGRESS, EnumSet.of(DeliverableStatus.SUBMITTED, DeliverableStatus.CANCELLED));
        ALLOWED.put(DeliverableStatus.SUBMITTED, EnumSet.of(
                DeliverableStatus.APPROVED, DeliverableStatus.REJECTED, DeliverableStatus.CANCELLED));
        ALLOWED.put(DeliverableStatus.REJECTED, EnumSet.of(DeliverableStatus.IN_PROGRESS, DeliverableStatus.CANCELLED));
        ALLOWED.put(DeliverableStatus.APPROVED, EnumSet.noneOf(DeliverableStatus.class));
        ALLOWED.put(DeliverableStatus.CANCELLED, EnumSet.noneOf(DeliverableStatus.class));
    }

    private DeliverableLifecycle() {}

    public static void assertTransition(DeliverableStatus from, DeliverableStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid deliverable status transition: " + from + " → " + to);
        }
    }
}
