package com.trustagro.training.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.training.entity.ProgramStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Program lifecycle (backend-authoritative).
 * <pre>
 * DRAFT → PUBLISHED → REGISTRATION_OPEN → REGISTRATION_CLOSED → IN_PROGRESS → COMPLETED
 *   └──────────────┴────────────────────┴─────────────────────┴─────────────→ CANCELLED
 * </pre>
 */
public final class TrainingProgramLifecycle {

    private static final Map<ProgramStatus, Set<ProgramStatus>> ALLOWED = new EnumMap<>(ProgramStatus.class);

    static {
        ALLOWED.put(ProgramStatus.DRAFT, EnumSet.of(ProgramStatus.PUBLISHED, ProgramStatus.CANCELLED));
        ALLOWED.put(ProgramStatus.PUBLISHED, EnumSet.of(ProgramStatus.REGISTRATION_OPEN, ProgramStatus.CANCELLED));
        ALLOWED.put(ProgramStatus.REGISTRATION_OPEN, EnumSet.of(ProgramStatus.REGISTRATION_CLOSED, ProgramStatus.IN_PROGRESS, ProgramStatus.CANCELLED));
        ALLOWED.put(ProgramStatus.REGISTRATION_CLOSED, EnumSet.of(ProgramStatus.IN_PROGRESS, ProgramStatus.CANCELLED));
        ALLOWED.put(ProgramStatus.IN_PROGRESS, EnumSet.of(ProgramStatus.COMPLETED, ProgramStatus.CANCELLED));
        ALLOWED.put(ProgramStatus.COMPLETED, EnumSet.noneOf(ProgramStatus.class));
        ALLOWED.put(ProgramStatus.CANCELLED, EnumSet.noneOf(ProgramStatus.class));
    }

    private TrainingProgramLifecycle() {}

    public static void assertTransition(ProgramStatus from, ProgramStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid program status transition: " + from + " → " + to);
        }
    }

    public static boolean isEditable(ProgramStatus status) {
        return status == ProgramStatus.DRAFT || status == ProgramStatus.PUBLISHED;
    }
}
