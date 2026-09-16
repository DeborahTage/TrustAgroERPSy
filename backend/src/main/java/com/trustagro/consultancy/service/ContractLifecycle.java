package com.trustagro.consultancy.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.consultancy.entity.ContractStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Contract lifecycle.
 * <pre>
 * DRAFT → PENDING_SIGNATURE → ACTIVE → EXPIRED | TERMINATED | COMPLETED | CANCELLED
 *   └───────────────────────→ CANCELLED
 * </pre>
 */
public final class ContractLifecycle {

    private static final Map<ContractStatus, Set<ContractStatus>> ALLOWED = new EnumMap<>(ContractStatus.class);

    static {
        ALLOWED.put(ContractStatus.DRAFT, EnumSet.of(ContractStatus.PENDING_SIGNATURE, ContractStatus.CANCELLED));
        ALLOWED.put(ContractStatus.PENDING_SIGNATURE, EnumSet.of(ContractStatus.ACTIVE, ContractStatus.CANCELLED));
        ALLOWED.put(ContractStatus.ACTIVE, EnumSet.of(
                ContractStatus.EXPIRED, ContractStatus.TERMINATED,
                ContractStatus.COMPLETED, ContractStatus.CANCELLED));
        ALLOWED.put(ContractStatus.EXPIRED, EnumSet.noneOf(ContractStatus.class));
        ALLOWED.put(ContractStatus.TERMINATED, EnumSet.noneOf(ContractStatus.class));
        ALLOWED.put(ContractStatus.COMPLETED, EnumSet.noneOf(ContractStatus.class));
        ALLOWED.put(ContractStatus.CANCELLED, EnumSet.noneOf(ContractStatus.class));
    }

    private ContractLifecycle() {}

    public static void assertTransition(ContractStatus from, ContractStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid contract status transition: " + from + " → " + to);
        }
    }

    public static boolean isEditable(ContractStatus status) {
        return status == ContractStatus.DRAFT || status == ContractStatus.PENDING_SIGNATURE;
    }
}
