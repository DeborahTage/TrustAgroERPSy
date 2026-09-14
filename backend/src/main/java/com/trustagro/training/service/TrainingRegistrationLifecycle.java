package com.trustagro.training.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.training.entity.RegistrationStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Registration lifecycle.
 * <pre>
 * PENDING → CONFIRMED → COMPLETED
 *   └──────┴──────────→ CANCELLED
 * </pre>
 */
public final class TrainingRegistrationLifecycle {

    private static final Map<RegistrationStatus, Set<RegistrationStatus>> ALLOWED = new EnumMap<>(RegistrationStatus.class);

    static {
        ALLOWED.put(RegistrationStatus.PENDING, EnumSet.of(RegistrationStatus.CONFIRMED, RegistrationStatus.CANCELLED));
        ALLOWED.put(RegistrationStatus.CONFIRMED, EnumSet.of(RegistrationStatus.COMPLETED, RegistrationStatus.CANCELLED));
        ALLOWED.put(RegistrationStatus.COMPLETED, EnumSet.noneOf(RegistrationStatus.class));
        ALLOWED.put(RegistrationStatus.CANCELLED, EnumSet.noneOf(RegistrationStatus.class));
    }

    private TrainingRegistrationLifecycle() {}

    public static void assertTransition(RegistrationStatus from, RegistrationStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid registration status transition: " + from + " → " + to);
        }
    }
}
