package com.trustagro.consultancy.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.consultancy.entity.ActivityStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Activity lifecycle.
 * <pre>
 * PLANNED → IN_PROGRESS → COMPLETED | CANCELLED
 *   └───────────────────→ COMPLETED | CANCELLED
 * </pre>
 */
public final class ActivityLifecycle {

    private static final Map<ActivityStatus, Set<ActivityStatus>> ALLOWED = new EnumMap<>(ActivityStatus.class);

    static {
        ALLOWED.put(ActivityStatus.PLANNED, EnumSet.of(
                ActivityStatus.IN_PROGRESS, ActivityStatus.COMPLETED, ActivityStatus.CANCELLED));
        ALLOWED.put(ActivityStatus.IN_PROGRESS, EnumSet.of(ActivityStatus.COMPLETED, ActivityStatus.CANCELLED));
        ALLOWED.put(ActivityStatus.COMPLETED, EnumSet.noneOf(ActivityStatus.class));
        ALLOWED.put(ActivityStatus.CANCELLED, EnumSet.noneOf(ActivityStatus.class));
    }

    private ActivityLifecycle() {}

    public static void assertTransition(ActivityStatus from, ActivityStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid activity status transition: " + from + " → " + to);
        }
    }
}
