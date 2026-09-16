package com.trustagro.procurement.service;

import com.trustagro.common.exception.BusinessException;
import com.trustagro.inventory.entity.PurchaseOrderStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * <pre>
 * DRAFT → SUBMITTED → APPROVED → SENT → PARTIALLY_RECEIVED → FULLY_RECEIVED → CLOSED
 * DRAFT/SUBMITTED/APPROVED/SENT → CANCELLED
 * FULLY_RECEIVED → CLOSED
 * </pre>
 */
public final class PurchaseOrderLifecycle {

    private static final Map<PurchaseOrderStatus, Set<PurchaseOrderStatus>> ALLOWED = new EnumMap<>(PurchaseOrderStatus.class);

    static {
        ALLOWED.put(PurchaseOrderStatus.DRAFT, EnumSet.of(
                PurchaseOrderStatus.SUBMITTED, PurchaseOrderStatus.CANCELLED));
        ALLOWED.put(PurchaseOrderStatus.SUBMITTED, EnumSet.of(
                PurchaseOrderStatus.APPROVED, PurchaseOrderStatus.CANCELLED));
        ALLOWED.put(PurchaseOrderStatus.APPROVED, EnumSet.of(
                PurchaseOrderStatus.SENT, PurchaseOrderStatus.CANCELLED));
        ALLOWED.put(PurchaseOrderStatus.SENT, EnumSet.of(
                PurchaseOrderStatus.PARTIALLY_RECEIVED, PurchaseOrderStatus.FULLY_RECEIVED,
                PurchaseOrderStatus.CANCELLED));
        ALLOWED.put(PurchaseOrderStatus.PARTIALLY_RECEIVED, EnumSet.of(
                PurchaseOrderStatus.PARTIALLY_RECEIVED, PurchaseOrderStatus.FULLY_RECEIVED,
                PurchaseOrderStatus.CLOSED));
        ALLOWED.put(PurchaseOrderStatus.FULLY_RECEIVED, EnumSet.of(
                PurchaseOrderStatus.CLOSED));
        ALLOWED.put(PurchaseOrderStatus.CANCELLED, EnumSet.noneOf(PurchaseOrderStatus.class));
        ALLOWED.put(PurchaseOrderStatus.CLOSED, EnumSet.noneOf(PurchaseOrderStatus.class));
    }

    private PurchaseOrderLifecycle() {}

    public static void assertTransition(PurchaseOrderStatus from, PurchaseOrderStatus to) {
        if (from == to && from == PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            return;
        }
        if (from == null || to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException("Invalid purchase order status transition: " + from + " → " + to);
        }
    }
}
