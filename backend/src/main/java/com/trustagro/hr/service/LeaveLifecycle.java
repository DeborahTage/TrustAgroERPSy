package com.trustagro.hr.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.hr.entity.LeaveRequestStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Leave request lifecycle (backend-authoritative).
 * <pre>
 * PENDING → APPROVED | REJECTED | CANCELLED
 * </pre>
 * Decisions are terminal — an approved request can never fall back to PENDING.
 */
public final class LeaveLifecycle {

    private static final Map<LeaveRequestStatus, Set<LeaveRequestStatus>> ALLOWED =
            new EnumMap<>(LeaveRequestStatus.class);

    static {
        ALLOWED.put(LeaveRequestStatus.PENDING, EnumSet.of(
                LeaveRequestStatus.APPROVED, LeaveRequestStatus.REJECTED, LeaveRequestStatus.CANCELLED));
        ALLOWED.put(LeaveRequestStatus.APPROVED, EnumSet.noneOf(LeaveRequestStatus.class));
        ALLOWED.put(LeaveRequestStatus.REJECTED, EnumSet.noneOf(LeaveRequestStatus.class));
        ALLOWED.put(LeaveRequestStatus.CANCELLED, EnumSet.noneOf(LeaveRequestStatus.class));
    }

    private LeaveLifecycle() {}

    public static void assertTransition(LeaveRequestStatus from, LeaveRequestStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid leave request status transition: " + from + " → " + to);
        }
    }

    public static boolean isEditable(LeaveRequestStatus status) {
        return status == LeaveRequestStatus.PENDING;
    }
}
