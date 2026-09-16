package com.trustagro.consultancy.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.consultancy.entity.RequestStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Request lifecycle.
 * <pre>
 * NEW → QUALIFYING → ASSIGNED → PROPOSAL_REQUIRED → CONVERTED
 *   └──────────────┴──────────┴──────────────────→ REJECTED | CANCELLED
 * </pre>
 */
public final class RequestLifecycle {

    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED = new EnumMap<>(RequestStatus.class);

    static {
        ALLOWED.put(RequestStatus.NEW, EnumSet.of(
                RequestStatus.QUALIFYING, RequestStatus.ASSIGNED, RequestStatus.PROPOSAL_REQUIRED,
                RequestStatus.CONVERTED, RequestStatus.REJECTED, RequestStatus.CANCELLED));
        ALLOWED.put(RequestStatus.QUALIFYING, EnumSet.of(
                RequestStatus.ASSIGNED, RequestStatus.PROPOSAL_REQUIRED, RequestStatus.CONVERTED,
                RequestStatus.REJECTED, RequestStatus.CANCELLED));
        ALLOWED.put(RequestStatus.ASSIGNED, EnumSet.of(
                RequestStatus.PROPOSAL_REQUIRED, RequestStatus.CONVERTED,
                RequestStatus.REJECTED, RequestStatus.CANCELLED));
        ALLOWED.put(RequestStatus.PROPOSAL_REQUIRED, EnumSet.of(
                RequestStatus.CONVERTED, RequestStatus.REJECTED, RequestStatus.CANCELLED));
        ALLOWED.put(RequestStatus.CONVERTED, EnumSet.noneOf(RequestStatus.class));
        ALLOWED.put(RequestStatus.REJECTED, EnumSet.noneOf(RequestStatus.class));
        ALLOWED.put(RequestStatus.CANCELLED, EnumSet.noneOf(RequestStatus.class));
    }

    private RequestLifecycle() {}

    public static void assertTransition(RequestStatus from, RequestStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid request status transition: " + from + " → " + to);
        }
    }
}
