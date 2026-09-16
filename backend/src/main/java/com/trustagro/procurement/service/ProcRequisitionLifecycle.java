package com.trustagro.procurement.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.procurement.entity.ProcRequisitionStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * DRAFT → SUBMITTED → UNDER_REVIEW → APPROVED → CONVERTED
 *                  └→ REJECTED | CANCELLED
 * DRAFT/SUBMITTED/UNDER_REVIEW/APPROVED → CANCELLED
 * </pre>
 */
public final class ProcRequisitionLifecycle {

    private static final Map<ProcRequisitionStatus, Set<ProcRequisitionStatus>> ALLOWED = new EnumMap<>(ProcRequisitionStatus.class);

    static {
        ALLOWED.put(ProcRequisitionStatus.DRAFT, EnumSet.of(
                ProcRequisitionStatus.SUBMITTED, ProcRequisitionStatus.CANCELLED));
        ALLOWED.put(ProcRequisitionStatus.SUBMITTED, EnumSet.of(
                ProcRequisitionStatus.UNDER_REVIEW, ProcRequisitionStatus.APPROVED,
                ProcRequisitionStatus.REJECTED, ProcRequisitionStatus.CANCELLED));
        ALLOWED.put(ProcRequisitionStatus.UNDER_REVIEW, EnumSet.of(
                ProcRequisitionStatus.APPROVED, ProcRequisitionStatus.REJECTED,
                ProcRequisitionStatus.CANCELLED));
        ALLOWED.put(ProcRequisitionStatus.APPROVED, EnumSet.of(
                ProcRequisitionStatus.CONVERTED, ProcRequisitionStatus.CANCELLED));
        ALLOWED.put(ProcRequisitionStatus.REJECTED, EnumSet.noneOf(ProcRequisitionStatus.class));
        ALLOWED.put(ProcRequisitionStatus.CANCELLED, EnumSet.noneOf(ProcRequisitionStatus.class));
        ALLOWED.put(ProcRequisitionStatus.CONVERTED, EnumSet.noneOf(ProcRequisitionStatus.class));
    }

    private ProcRequisitionLifecycle() {}

    public static void assertTransition(ProcRequisitionStatus from, ProcRequisitionStatus to) {
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid requisition status transition: " + from + " → " + to);
        }
    }
}
