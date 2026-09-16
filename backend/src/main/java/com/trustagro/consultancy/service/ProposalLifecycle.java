package com.trustagro.consultancy.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.consultancy.entity.ProposalStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Proposal lifecycle.
 * <pre>
 * DRAFT → SENT → ACCEPTED | REJECTED | EXPIRED | CANCELLED
 *   └──────────→ CANCELLED
 * </pre>
 */
public final class ProposalLifecycle {

    private static final Map<ProposalStatus, Set<ProposalStatus>> ALLOWED = new EnumMap<>(ProposalStatus.class);

    static {
        ALLOWED.put(ProposalStatus.DRAFT, EnumSet.of(ProposalStatus.SENT, ProposalStatus.CANCELLED));
        ALLOWED.put(ProposalStatus.SENT, EnumSet.of(
                ProposalStatus.ACCEPTED, ProposalStatus.REJECTED,
                ProposalStatus.EXPIRED, ProposalStatus.CANCELLED));
        ALLOWED.put(ProposalStatus.ACCEPTED, EnumSet.noneOf(ProposalStatus.class));
        ALLOWED.put(ProposalStatus.REJECTED, EnumSet.noneOf(ProposalStatus.class));
        ALLOWED.put(ProposalStatus.EXPIRED, EnumSet.noneOf(ProposalStatus.class));
        ALLOWED.put(ProposalStatus.CANCELLED, EnumSet.noneOf(ProposalStatus.class));
    }

    private ProposalLifecycle() {}

    public static void assertTransition(ProposalStatus from, ProposalStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid proposal status transition: " + from + " → " + to);
        }
    }

    public static boolean isEditable(ProposalStatus status) {
        return status == ProposalStatus.DRAFT;
    }
}
